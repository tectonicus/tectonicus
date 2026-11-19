// Generates LevelDB fixture databases for tectonicus.bedrock.leveldb tests.
// Usage: npm install classic-level@1 && node gen.js <outDir>
const { ClassicLevel } = require('classic-level');
const fs = require('fs');
const path = require('path');

const out = process.argv[2];
fs.rmSync(out, { recursive: true, force: true });
fs.mkdirSync(out, { recursive: true });

const enc = { keyEncoding: 'buffer', valueEncoding: 'buffer' };
const key = (s) => Buffer.from(s, 'latin1');
const val = (s, n) => Buffer.from((s + '|').repeat(n), 'latin1');

function writeExpected(dir, model) {
	const keys = [...model.keys()].sort((a, b) => Buffer.compare(Buffer.from(a, 'hex'), Buffer.from(b, 'hex')));
	const lines = keys.map((k) => k + '\t' + model.get(k).toString('hex'));
	fs.writeFileSync(dir + '.expected.tsv', lines.join('\n') + '\n');
}

function cleanup(dir) {
	for (const f of fs.readdirSync(dir)) {
		if (f === 'LOG' || f === 'LOG.old') fs.rmSync(path.join(dir, f));
	}
}

async function open(dir, compression) {
	const db = new ClassicLevel(dir, { ...enc, compression, blockSize: 512, writeBufferSize: 64 * 1024 });
	await db.open();
	return db;
}

async function put(db, model, k, v) {
	await db.put(k, v);
	model.set(k.toString('hex'), v);
}

async function del(db, model, k) {
	await db.del(k);
	model.delete(k.toString('hex'));
}

async function memtableOnly(dir) {
	const model = new Map();
	const db = await open(dir, true);
	for (let i = 0; i < 200; i++) {
		await put(db, model, key('mem' + String(i).padStart(4, '0')), val('value' + i, 1 + (i % 7)));
	}
	for (let i = 0; i < 200; i += 10) {
		await del(db, model, key('mem' + String(i).padStart(4, '0')));
	}
	for (let i = 5; i < 200; i += 20) {
		await put(db, model, key('mem' + String(i).padStart(4, '0')), val('overwritten' + i, 2));
	}
	// a batch with puts and deletes
	const ops = [];
	for (let i = 0; i < 20; i++) {
		const k = key('batch' + String(i).padStart(3, '0'));
		if (i % 4 === 3) {
			ops.push({ type: 'del', key: key('batch' + String(i - 1).padStart(3, '0')) });
		} else {
			ops.push({ type: 'put', key: k, value: val('b' + i, 3) });
		}
	}
	await db.batch(ops);
	for (const op of ops) {
		if (op.type === 'put') model.set(op.key.toString('hex'), op.value);
		else model.delete(op.key.toString('hex'));
	}
	await db.close();
	cleanup(dir);
	writeExpected(dir, model);
}

function binKey(i) {
	// Bedrock-like binary key: x(int32 LE), z(int32 LE), tag byte
	const b = Buffer.alloc(9);
	b.writeInt32LE((i % 37) - 18, 0);
	b.writeInt32LE(Math.floor(i / 37) - 5, 4);
	b[8] = 0x2f + (i % 3);
	return b;
}

async function leveled(dir, compression) {
	const model = new Map();
	let db = await open(dir, compression);
	// Phase 1: bulk data, compacted down to deeper levels
	for (let i = 0; i < 600; i++) {
		await put(db, model, key('key' + String(i).padStart(5, '0')), val('v1-' + i, 4 + (i % 5)));
	}
	for (let i = 0; i < 150; i++) {
		await put(db, model, binKey(i), val('bin' + i, 3));
	}
	await db.compactRange(Buffer.alloc(0), Buffer.from([0xff, 0xff, 0xff, 0xff]));
	await db.close();

	// Phase 2: overwrites and deletes, flushed to level 0 on reopen (log recovery)
	db = await open(dir, compression);
	for (let i = 0; i < 100; i++) {
		await put(db, model, key('key' + String(i).padStart(5, '0')), val('v2-' + i, 2));
	}
	for (let i = 100; i < 160; i++) {
		await del(db, model, key('key' + String(i).padStart(5, '0')));
	}
	for (let i = 0; i < 150; i += 5) {
		await del(db, model, binKey(i));
	}
	await put(db, model, key('zzz-new-in-l0'), val('l0', 3));
	await db.close();
	db = await open(dir, compression);
	// Phase 2b: second overlapping level-0 table
	for (let i = 0; i < 20; i++) {
		await put(db, model, key('key' + String(i).padStart(5, '0')), val('v2b-' + i, 2));
	}
	for (let i = 20; i < 30; i++) {
		await del(db, model, key('key' + String(i).padStart(5, '0')));
	}
	await db.close();
	db = await open(dir, compression);
	await db.close();

	// Phase 3: more changes left in the log (memtable)
	db = await open(dir, compression);
	for (let i = 50; i < 70; i++) {
		await put(db, model, key('key' + String(i).padStart(5, '0')), val('v3-' + i, 1));
	}
	for (let i = 100; i < 110; i++) {
		await put(db, model, key('key' + String(i).padStart(5, '0')), val('resurrected-' + i, 1));
	}
	for (let i = 500; i < 520; i++) {
		await del(db, model, key('key' + String(i).padStart(5, '0')));
	}
	await put(db, model, key(''), val('empty-key', 1));
	await put(db, model, key('empty-value'), Buffer.alloc(0));
	await put(db, model, key('big-value'), Buffer.from(Array.from({ length: 5000 }, (_, i) => (i * 7) & 0xff)));
	await db.close();
	cleanup(dir);
	writeExpected(dir, model);
}

(async () => {
	await memtableOnly(path.join(out, 'memtable'));
	await leveled(path.join(out, 'leveled-none'), false);
})().catch((e) => {
	console.error(e);
	process.exit(1);
});
