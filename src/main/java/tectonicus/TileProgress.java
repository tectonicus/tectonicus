/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import tectonicus.configuration.Dimension;

import java.io.Console;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

/**
 * Console progress for base tile rendering: a small map of the tiles, a legend, and a progress bar with
 * the rate and estimated time remaining.
 * <p>
 * The map covers every visible tile, so tiles left unchanged since the last run are shown too, and is scaled
 * down to fit the terminal. Each character covers a block of tiles twice as tall as it is wide, because
 * terminal characters are about twice as tall as they are wide, so the map keeps the shape of the real one.
 * <p>
 * In an interactive terminal the map and bar are redrawn in place: the map after each character's worth of
 * tiles, the bar every second. Elsewhere, such as an IDE console or a redirected stream, the bar is updated on
 * one line and the map is printed now and then. Other console output, such as log messages, clears the display
 * before it is written and the display is drawn again below it, so nothing is drawn over it. Colours are used
 * unless {@code NO_COLOR} is set or {@code TERM} is {@code dumb}, and block characters are used when the
 * output encoding can represent them, with plain ASCII otherwise.
 * <p>
 * Call {@link #started} and {@link #finished} from the rendering loop; other threads may write to the console.
 */
public final class TileProgress implements AutoCloseable {
	/** Used when the terminal size is unknown; fits a standard 80x24 terminal. */
	static final int DEFAULT_COLUMNS = 80;
	static final int DEFAULT_LINES = 24;
	/** Lines left free around the map: heading, legend, progress bar, and some of the output above. */
	private static final int RESERVED_LINES = 7;
	/** How often the time left is updated. */
	static final long STATUS_NANOS = 1_000_000_000L;
	/** The map is redrawn when a character of it changes, but not more often than this. */
	static final long MIN_MAP_NANOS = 1_000_000_000L;
	/** How often the map is printed again when it cannot be redrawn in place. */
	static final long MAP_INTERVAL_NANOS = 30_000_000_000L;

	private static final String ESC = "\u001b[";
	static final String RESET = ESC + "0m";
	static final String BOLD = ESC + "1m";
	static final String GREEN = ESC + "32m";
	static final String YELLOW = ESC + "33m";
	static final String CYAN = ESC + "36m";
	static final String GREY = ESC + "90m";
	static final String BRIGHT_MAGENTA = ESC + "95m";
	static final String BRIGHT_CYAN = ESC + "96m";
	/** 256-colour dark green, for tiles rendered on an earlier run. */
	static final String DARK_GREEN = ESC + "38;5;22m";
	static final String DARK_GREEN_BACKGROUND = ESC + "48;5;22m";
	static final String WHITE = ESC + "97m";
	private static final String CLEAR_LINE = ESC + "2K";
	private static final String CLEAR_TO_END = ESC + "J";

	/** What a map character shows, from the tiles it covers. */
	enum Cell { EMPTY, UNCHANGED, TO_RENDER, PARTLY, MOSTLY, DONE, CURRENT }

	/** How progress is written: colours, redrawing in place, the width available, and the characters used. */
	record Style(boolean colour, boolean live, int columns, Glyphs glyphs, Theme theme) {
		Style(boolean colour, boolean live, int columns, Glyphs glyphs) {
			this(colour, live, columns, glyphs, Theme.OVERWORLD);
		}
	}

	/**
	 * Colours that suit the dimension: rendered and unchanged tiles, the shade background behind areas with
	 * some changed tiles, and the tile being rendered, chosen to stand out from the rest.
	 */
	record Theme(String rendered, String unchanged, String unchangedBackground, String current) {
		static final Theme OVERWORLD = new Theme(GREEN, DARK_GREEN, DARK_GREEN_BACKGROUND, BOLD + BRIGHT_MAGENTA);
		static final Theme NETHER = new Theme(ESC + "38;5;160m", ESC + "38;5;52m", ESC + "48;5;52m", BOLD + BRIGHT_CYAN);
		static final Theme END = new Theme(ESC + "38;5;141m", ESC + "38;5;54m", ESC + "48;5;54m", BOLD + BRIGHT_CYAN);

		static Theme of(Dimension dimension) {
			if (dimension == null)
				return OVERWORLD;
			return switch (dimension) {
				case NETHER -> NETHER;
				case END -> END;
				default -> OVERWORLD;
			};
		}
	}

	/** The characters used, chosen from what the output encoding can represent. */
	record Glyphs(char unchanged, char toRender, char partly, char mostly, char done, char current,
				  char barFull, char barEmpty, String barEighths, String barStart, String barEnd, char times) {

		/** Block characters where possible, falling back to ASCII one character at a time. */
		static Glyphs forCharset(Charset charset) {
			CharsetEncoder encoder = charset.newEncoder();
			String eighths = encoder.canEncode("▏▎▍▌▋▊▉") ? "▏▎▍▌▋▊▉" : "";
			boolean blocks = encoder.canEncode("█░");
			return new Glyphs(
					pick(encoder, '·', '.'),
					pick(encoder, '░', '-'),
					pick(encoder, '▒', ':'),
					pick(encoder, '▓', '+'),
					pick(encoder, '█', '#'),
					pick(encoder, '■', '@'),
					blocks ? '█' : '#',
					blocks ? '░' : '.',
					blocks ? eighths : "",
					blocks ? "" : "[",
					blocks ? "" : "]",
					pick(encoder, '×', 'x'));
		}

		static Glyphs ascii() {
			return new Glyphs('.', '-', ':', '+', '#', '@', '#', '.', "", "[", "]", 'x');
		}

		private static char pick(CharsetEncoder encoder, char preferred, char fallback) {
			return encoder.canEncode(preferred) ? preferred : fallback;
		}
	}

	private final PrintStream out;
	private final Style style;
	private final LongSupplier nanoClock;
	private final long startNanos;

	private final int total;
	private final int unchanged;
	private int done;
	private TileCoord current;

	private final int minX, minY;
	private final int cellWidth, cellHeight;
	private final int columns, rows;
	private final int[] cellVisible;
	private final int[] cellTotals;
	private final int[] cellDone;
	private int currentCell = -1;


	/** Guards the display, which other threads clear when they write to the console. */
	private final Object lock = new Object();
	private boolean completed;
	/** Lines of the live display on screen, so the next redraw can move back over them; 0 when there is none. */
	private int liveLines;
	/** Whether the progress line is showing without a line break after it, when not drawing live. */
	private boolean statusOnScreen;
	private boolean mapPrinted;
	/** Whether other output has left the cursor partway along a line. */
	private boolean foreignLineOpen;
	private long lastMapNanos;
	private long lastStatusNanos;
	/** Whether a character of the map has changed since it was last drawn. */
	private boolean mapChanged;
	private PrintStream originalOut, originalErr;

	/**
	 * @param visible every tile in the map, whether it will be rendered or not
	 * @param toRender the tiles that will be rendered, all of them also in {@code visible}
	 * @param min the smallest x and y of the visible tiles, max the largest
	 * @param maxColumns, maxRows the largest map that fits, in characters
	 */
	TileProgress(Iterable<TileCoord> visible, Iterable<TileCoord> toRender, TileCoord min, TileCoord max,
				 int maxColumns, int maxRows, PrintStream out, Style style, LongSupplier nanoClock) {
		this.out = out;
		this.style = style;
		this.nanoClock = nanoClock;
		this.startNanos = nanoClock.getAsLong();

		if (min.x > max.x || min.y > max.y) {
			minX = minY = 0;
			cellWidth = 1;
			cellHeight = 2;
			columns = rows = 0;
			cellVisible = cellTotals = cellDone = new int[0];
		} else {
			minX = min.x;
			minY = min.y;
			final long width = (long) max.x - min.x + 1;
			final long height = (long) max.y - min.y + 1;
			// The smallest scale at which the map fits: s tiles wide and 2s tiles tall per character.
			final long scale = Math.max(1, Math.max(ceilDiv(width, Math.max(1, maxColumns)), ceilDiv(height, 2L * Math.max(1, maxRows))));
			cellWidth = (int) Math.min(Integer.MAX_VALUE, scale);
			cellHeight = (int) Math.min(Integer.MAX_VALUE, scale * 2);
			columns = (int) ceilDiv(width, cellWidth);
			rows = (int) ceilDiv(height, cellHeight);
			cellVisible = new int[columns * rows];
			cellTotals = new int[columns * rows];
			cellDone = new int[columns * rows];
		}

		int visibleCount = 0;
		for (TileCoord tile : visible) {
			visibleCount++;
			int cell = cellOf(tile);
			if (cell >= 0)
				cellVisible[cell]++;
		}
		int toRenderCount = 0;
		for (TileCoord tile : toRender) {
			toRenderCount++;
			int cell = cellOf(tile);
			if (cell >= 0)
				cellTotals[cell]++;
		}
		this.total = toRenderCount;
		this.unchanged = Math.max(0, visibleCount - toRenderCount);
	}

	/**
	 * Writes to standard output, sized for the terminal and styled for what it supports. Until {@link #close()},
	 * anything else written to standard output or standard error, such as log messages, is printed above the
	 * display rather than over it. Colours follow the dimension: green for the overworld, red for the nether and
	 * purple for the end.
	 */
	public static TileProgress forConsole(Iterable<TileCoord> visible, Iterable<TileCoord> toRender, TileCoord min, TileCoord max,
										  Dimension dimension) {
		int terminalColumns = Math.clamp(environmentSize("COLUMNS", DEFAULT_COLUMNS), 40, 400);
		int terminalLines = Math.clamp(environmentSize("LINES", DEFAULT_LINES), 10, 200);
		boolean colour = System.getenv("NO_COLOR") == null && !"dumb".equals(System.getenv("TERM"));
		Style style = new Style(colour, colour && isInteractiveTerminal(), terminalColumns, Glyphs.forCharset(System.out.charset()),
				Theme.of(dimension));
		int maxColumns = Math.clamp(terminalColumns - 2, 20, 200);
		int maxRows = Math.clamp(terminalLines - RESERVED_LINES, 4, 80);
		TileProgress progress = new TileProgress(visible, toRender, min, max, maxColumns, maxRows, System.out, style, System::nanoTime);
		progress.guardConsole();
		return progress;
	}

	private static int environmentSize(String name, int fallback) {
		try {
			String value = System.getenv(name);
			return value == null ? fallback : Integer.parseInt(value.trim());
		} catch (RuntimeException e) {
			return fallback;
		}
	}

	/** A terminal that can move the cursor, rather than an IDE console, pipe or file. */
	private static boolean isInteractiveTerminal() {
		Console console = System.console();
		if (console == null)
			return false;
		try {
			// From Java 22 a console is returned even when output is redirected; isTerminal tells them apart.
			Method isTerminal = Console.class.getMethod("isTerminal");
			return (Boolean) isTerminal.invoke(console);
		} catch (NoSuchMethodException e) {
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}

	public int total() {
		return total;
	}

	public int unchanged() {
		return unchanged;
	}

	/** Marks the tile now being rendered and refreshes the display when it is due. */
	public void started(TileCoord tile) {
		synchronized (lock) {
			current = tile;
			int cell = cellOf(tile);
			if (cell != currentCell)
				mapChanged = true;
			currentCell = cell;
			if (completed)
				return;
			long now = nanoClock.getAsLong();
			endForeignLine();
			if (style.live()) {
				// The map is redrawn when it looks different; the progress bar and time left every second.
				boolean mapDue = liveLines == 0 || (mapChanged && now - lastMapNanos >= MIN_MAP_NANOS);
				if (mapDue)
					drawLive(now);
				else if (now - lastStatusNanos >= STATUS_NANOS)
					drawLiveStatus(now);
				else
					return;
			} else {
				boolean mapDue = !mapPrinted || now - lastMapNanos >= MAP_INTERVAL_NANOS;
				if (mapDue) {
					if (statusOnScreen)
						out.println();
					statusOnScreen = false;
					for (String line : mapFrame())
						out.println(line);
					mapPrinted = true;
					lastMapNanos = now;
				}
				if (!statusOnScreen || now - lastStatusNanos >= STATUS_NANOS)
					printStatus(now);
				else
					return;
			}
			out.flush();
		}
	}

	/** Marks a tile as rendered. */
	public void finished(TileCoord tile) {
		synchronized (lock) {
			done++;
			int cell = cellOf(tile);
			if (cell >= 0) {
				Look before = look(cell);
				cellDone[cell]++;
				if (cell == currentCell)
					currentCell = -1;
				if (!look(cell).equals(before))
					mapChanged = true;
			}
			current = null;
		}
	}

	/** Shows the final map and progress, leaving them on screen, and returns a one-line summary for the log. */
	public String complete() {
		synchronized (lock) {
			if (!completed) {
				currentCell = -1;
				current = null;
				endForeignLine();
				if (style.live()) {
					drawLive(nanoClock.getAsLong());
					out.println();
				} else {
					if (statusOnScreen)
						out.println();
					for (String line : mapFrame())
						out.println(line);
					out.println(statusLine());
				}
				finish();
			}
			return summary();
		}
	}

	/**
	 * Stops updating the display, leaving whatever is on screen, and restores standard output and error.
	 * Call it even when rendering fails, so later output, such as the error, is not treated as interrupting it.
	 */
	@Override
	public void close() {
		synchronized (lock) {
			if (!completed) {
				endForeignLine();
				if (liveLines > 0 || statusOnScreen)
					out.println();
				finish();
			}
			if (originalOut != null) {
				System.setOut(originalOut);
				System.setErr(originalErr);
				originalOut = originalErr = null;
			}
		}
	}

	private void finish() {
		completed = true;
		liveLines = 0;
		statusOnScreen = false;
		out.flush();
	}

	/** For example "rendered 2,000 tiles in 1m 49s (18.3 tiles/s), 12,000 unchanged". */
	String summary() {
		long elapsed = nanoClock.getAsLong() - startNanos;
		StringBuilder summary = new StringBuilder("rendered ");
		if (done < total)
			summary.append(number(done)).append(" of ");
		summary.append(number(total)).append(total == 1 ? " tile" : " tiles")
				.append(" in ").append(formatDuration(elapsed / 1_000_000_000L));
		String rate = rate();
		if (rate != null)
			summary.append(" (").append(rate).append(')');
		if (unchanged > 0)
			summary.append(", ").append(number(unchanged)).append(" unchanged");
		return summary.toString();
	}

	/** Draws the whole live display, over the previous one if it is still on screen. */
	private void drawLive(long now) {
		List<String> lines = liveFrame();
		StringBuilder frame = new StringBuilder();
		frame.append('\r');
		if (liveLines > 1)
			frame.append(ESC).append(liveLines - 1).append('A');
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0)
				frame.append("\r\n");
			frame.append(CLEAR_LINE).append(lines.get(i));
		}
		out.print(frame);
		liveLines = lines.size();
		lastMapNanos = lastStatusNanos = now;
		mapChanged = false;
	}

	/** Redraws just the progress bar, the last line of the live display, where the cursor is. */
	private void drawLiveStatus(long now) {
		out.print("\r" + CLEAR_LINE + statusLine());
		lastStatusNanos = now;
	}

	private void printStatus(long now) {
		// Pad over whatever is left of a longer previous line.
		Line status = status();
		out.print("\r" + status + " ".repeat(Math.max(0, style.columns() - 1 - status.visible)));
		statusOnScreen = true;
		lastStatusNanos = now;
	}

	/** Moves past a line someone else started but did not finish, so the display does not continue it. */
	private void endForeignLine() {
		if (foreignLineOpen) {
			out.println();
			foreignLineOpen = false;
		}
	}

	/** Sends standard output and error through {@link #guard}, until {@link #close()}. */
	private void guardConsole() {
		originalOut = System.out;
		originalErr = System.err;
		System.setOut(new PrintStream(guard(originalOut), true, originalOut.charset()));
		System.setErr(new PrintStream(guard(originalErr), true, originalErr.charset()));
	}

	/**
	 * A stream to {@code target} for other output, such as log messages, that clears the display before
	 * writing, so the output appears in full in its place and the display is drawn again below it.
	 */
	OutputStream guard(OutputStream target) {
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				write(new byte[] {(byte) b}, 0, 1);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				foreignWrite(target, b, off, len);
			}

			@Override
			public void flush() throws IOException {
				target.flush();
			}
		};
	}

	private void foreignWrite(OutputStream target, byte[] b, int off, int len) throws IOException {
		if (len == 0)
			return;
		synchronized (lock) {
			if (!completed)
				eraseDisplay();
			target.write(b, off, len);
			target.flush();
			// The display is drawn again at the next tile, so a burst of messages is not interleaved with it.
			foreignLineOpen = b[off + len - 1] != '\n';
		}
	}

	/** Removes the live display or the progress line, leaving the cursor at the start of the line it began on. */
	private void eraseDisplay() {
		if (foreignLineOpen)
			return;
		if (style.live() && liveLines > 0) {
			StringBuilder erase = new StringBuilder("\r");
			if (liveLines > 1)
				erase.append(ESC).append(liveLines - 1).append('A');
			erase.append(CLEAR_TO_END);
			out.print(erase);
			liveLines = 0;
		} else if (!style.live() && statusOnScreen) {
			out.print("\r" + " ".repeat(style.columns() - 1) + "\r");
			statusOnScreen = false;
		}
		out.flush();
	}

	List<String> liveFrame() {
		List<String> lines = mapFrame();
		lines.add(statusLine());
		return lines;
	}

	/** The heading, map and legend; just the heading if there is no map. */
	List<String> mapFrame() {
		List<String> lines = new ArrayList<>();
		lines.add(heading());
		for (int row = 0; row < rows; row++)
			lines.add(mapRow(row));
		if (columns > 0)
			lines.addAll(legend());
		return lines;
	}

	private String heading() {
		Line line = new Line();
		line.styled(BOLD, "Base tiles");
		line.plain("  " + number(total) + " to render");
		if (unchanged > 0)
			line.plain(", " + number(unchanged) + " unchanged");
		if (columns > 0)
			line.styled(GREY, "   1 char = " + cellWidth + style.glyphs().times() + cellHeight + " tiles");
		return line.toString();
	}

	String mapRow(int row) {
		Line line = new Line();
		String colour = null;
		StringBuilder run = new StringBuilder();
		// No trailing spaces, so narrow terminals do not wrap blank space onto the next line.
		int end = columns;
		while (end > 0 && cell(row * columns + end - 1) == Cell.EMPTY)
			end--;
		for (int column = 0; column < end; column++) {
			Look look = look(row * columns + column);
			if (!java.util.Objects.equals(look.colour(), colour)) {
				line.styled(colour, run.toString());
				run.setLength(0);
				colour = look.colour();
			}
			run.append(look.glyph());
		}
		line.styled(colour, run.toString());
		return line.toString();
	}

	/** What the map's characters mean, in one or two lines that do not change while rendering. */
	private List<String> legend() {
		Glyphs g = style.glyphs();
		List<String> lines = new ArrayList<>();
		if (style.colour()) {
			Line area = new Line();
			if (unchanged > 0)
				area.styled(style.theme().unchanged(), String.valueOf(g.done())).plain(" unchanged   ");
			area.styled(style.theme().unchangedBackground(), "" + g.unchanged() + g.toRender() + g.partly() + g.mostly())
					.plain(g.done() + " few to all tiles changed, which are:");
			lines.add(area.toString());
			Line states = new Line();
			states.plain("  ").plain(g.done() + " to render  ")
					.styled(YELLOW, String.valueOf(g.done())).plain(" rendering  ")
					.styled(style.theme().rendered(), String.valueOf(g.done())).plain(" rendered  ")
					.styled(style.theme().current(), String.valueOf(glyph(Cell.CURRENT))).plain(" now");
			lines.add(states.toString());
		} else {
			Line line = new Line();
			line.plain(g.done() + " rendered  " + g.partly() + g.mostly() + " partly  " + g.toRender() + " to render  ");
			if (unchanged > 0)
				line.plain(g.unchanged() + " unchanged  ");
			line.plain(g.current() + " now");
			lines.add(line.toString());
		}
		return lines;
	}

	/** For example "████████▋░░░░░░░░░░  45.2%  904/2,000  18.3 tiles/s  1m 00s left  @ 12,-3". */
	String statusLine() {
		return status().toString();
	}

	private Line status() {
		String percent = String.format(Locale.ROOT, "%5.1f%%", total == 0 ? 100.0 : 100.0 * done / total);
		String count = number(done) + "/" + number(total);
		String rate = rate();
		String remaining = remainingTime();
		String time = remaining != null ? remaining + " left"
				: done >= total ? "done" : null;
		String where = current != null ? "@ " + current.x + "," + current.y : null;

		// Leave the last column free, since writing to it makes some terminals wrap.
		int available = style.columns() - 1;
		List<String> extras = new ArrayList<>();
		if (rate != null) extras.add(rate);
		if (time != null) extras.add(time);
		if (where != null) extras.add(where);
		final int minBar = 10, maxBar = 40;
		Glyphs g = style.glyphs();
		int barEnds = g.barStart().length() + g.barEnd().length();
		int fixed = barEnds + 1 + percent.length() + 2 + count.length();
		while (!extras.isEmpty() && fixed + extrasLength(extras) + minBar > available)
			extras.removeLast();
		int barWidth = Math.clamp(available - fixed - extrasLength(extras), minBar, maxBar);

		Line line = new Line();
		bar(line, barWidth);
		line.plain(" ").styled(BOLD, percent).plain("  " + count);
		for (int i = 0; i < extras.size(); i++) {
			String extra = extras.get(i);
			line.plain("  ");
			if (extra == time)
				line.styled(CYAN, extra);
			else if (extra == where)
				line.styled(GREY, extra);
			else
				line.plain(extra);
		}
		return line;
	}

	private static int extrasLength(List<String> extras) {
		return extras.stream().mapToInt(extra -> extra.length() + 2).sum();
	}

	private void bar(Line line, int width) {
		Glyphs g = style.glyphs();
		double fraction = total == 0 ? 1 : (double) done / total;
		int steps = g.barEighths().isEmpty() ? 1 : 8;
		long filledSteps = Math.round(fraction * width * steps);
		int full = (int) (filledSteps / steps);
		int part = (int) (filledSteps % steps);
		StringBuilder filled = new StringBuilder();
		filled.append(String.valueOf(g.barFull()).repeat(full));
		if (part > 0)
			filled.append(g.barEighths().charAt(part - 1));
		int empty = width - filled.length();
		line.plain(g.barStart()).styled(style.theme().rendered(), filled.toString())
				.styled(GREY, String.valueOf(g.barEmpty()).repeat(Math.max(0, empty))).plain(g.barEnd());
	}

	/** Tiles per second so far, or null until there is enough to go on. */
	String rate() {
		long elapsed = nanoClock.getAsLong() - startNanos;
		if (done == 0 || elapsed < 1_000_000_000L)
			return null;
		double perSecond = done * 1e9 / elapsed;
		return String.format(Locale.ROOT, perSecond < 10 ? "%.2f tiles/s" : "%.1f tiles/s", perSecond);
	}

	/** Estimated time left from the average time per tile so far, or null until there is enough to go on. */
	String remainingTime() {
		long elapsed = nanoClock.getAsLong() - startNanos;
		if (done == 0 || done >= total || elapsed < 1_000_000_000L)
			return null;
		long remainingNanos = (long) ((double) elapsed / done * (total - done));
		return formatDuration(remainingNanos / 1_000_000_000L);
	}

	static String formatDuration(long seconds) {
		if (seconds < 60)
			return seconds + "s";
		if (seconds < 3600)
			return String.format(Locale.ROOT, "%dm %02ds", seconds / 60, seconds % 60);
		return String.format(Locale.ROOT, "%dh %02dm", seconds / 3600, seconds / 60 % 60);
	}

	private static String number(long value) {
		return String.format(Locale.ROOT, "%,d", value);
	}

	Cell cell(int cell) {
		if (cell == currentCell)
			return Cell.CURRENT;
		int cellTotal = cellTotals[cell];
		if (cellTotal == 0)
			return cellVisible[cell] == 0 ? Cell.EMPTY : Cell.UNCHANGED;
		int cellFinished = cellDone[cell];
		if (cellFinished >= cellTotal)
			return Cell.DONE;
		if (cellFinished == 0)
			return Cell.TO_RENDER;
		return cellFinished * 2 < cellTotal ? Cell.PARTLY : Cell.MOSTLY;
	}

	/** A map character and its colour codes, null for none. */
	record Look(char glyph, String colour) {}

	/**
	 * With colour, a character shows unchanged tiles as dark green, and over that a shade for the share of its
	 * tiles that changed, coloured by how far rendering them has got. Without colour it shows only the progress
	 * of the changed tiles, or that none changed.
	 */
	Look look(int cell) {
		if (!style.colour() || cellVisible[cell] == 0 || cell == currentCell || cellTotals[cell] == 0) {
			Cell state = cell(cell);
			return new Look(glyph(state), style.colour() ? colour(state) : null);
		}
		Glyphs g = style.glyphs();
		int visible = cellVisible[cell];
		int changed = Math.min(cellTotals[cell], visible);
		int rendered = cellDone[cell];
		String foreground = rendered == 0 ? null : rendered >= changed ? style.theme().rendered() : YELLOW;
		if (changed >= visible)
			return new Look(g.done(), foreground);
		double share = (double) changed / visible;
		char shade = share <= 0.1 ? g.unchanged()
				: share <= 0.35 ? g.toRender()
				: share <= 0.65 ? g.partly()
				: g.mostly();
		return new Look(shade, (foreground == null ? WHITE : foreground) + style.theme().unchangedBackground());
	}

	private char glyph(Cell cell) {
		Glyphs g = style.glyphs();
		return switch (cell) {
			case EMPTY -> ' ';
			// With colour, unchanged tiles look like rendered ones in a darker green; without, they need their own mark.
			case UNCHANGED -> style.colour() ? g.done() : g.unchanged();
			case TO_RENDER -> g.toRender();
			case PARTLY -> g.partly();
			case MOSTLY -> g.mostly();
			case DONE -> g.done();
			// With colour, a full block like the rest of the map; without, a mark that stands out from them.
			case CURRENT -> style.colour() ? g.done() : g.current();
		};
	}

	private String colour(Cell cell) {
		return switch (cell) {
			case EMPTY, TO_RENDER -> null;
			case UNCHANGED -> style.theme().unchanged();
			case PARTLY, MOSTLY -> YELLOW;
			case DONE -> style.theme().rendered();
			case CURRENT -> style.theme().current();
		};
	}

	private int cellOf(TileCoord tile) {
		if (columns == 0)
			return -1;
		long column = ((long) tile.x - minX) / cellWidth;
		long row = ((long) tile.y - minY) / cellHeight;
		if (tile.x < minX || tile.y < minY || column >= columns || row >= rows)
			return -1;
		return (int) (row * columns + column);
	}

	private static long ceilDiv(long value, long divisor) {
		return (value + divisor - 1) / divisor;
	}

	/** Text with optional colours, counting only the characters that take up space. */
	private final class Line {
		private final StringBuilder text = new StringBuilder();
		private int visible;

		Line plain(String s) {
			text.append(s);
			visible += s.length();
			return this;
		}

		Line styled(String colour, String s) {
			if (s.isEmpty())
				return this;
			if (colour == null || !style.colour())
				return plain(s);
			text.append(colour).append(s).append(RESET);
			visible += s.length();
			return this;
		}

		@Override
		public String toString() {
			return text.toString();
		}
	}
}
