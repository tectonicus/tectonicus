/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

public class FileBrowserLine extends JPanel
{
	private static final long serialVersionUID = 1L;

	private Frame parent;
	
	private JTextField textField;
	private JButton browseButton;
	
	private JLabel nameLabel;
	private JLabel infoLabel;
	
	private File file;
	
	private boolean ignoreTextEvents;
	
	private JFileChooser browseDialog;
	private String browseLabel;
	
	private FileLineListener listener;
	
	public FileBrowserLine(Frame parent, String label, String browseLabel, File initialDir, final int mode, FileFilter fileFilter)
	{
		this.parent = parent;
		
		if (initialDir != null)
			this.file = new File(initialDir.getAbsolutePath());
		else
			this.file = new File(".");
		
		this.browseLabel = browseLabel;
		
		setLayout( new BorderLayout() );
		
		nameLabel = new JLabel(label);
		add(nameLabel, BorderLayout.WEST);
		
		textField = new JTextField();
		if (initialDir != null)
			textField.setText(initialDir.getAbsolutePath());
		add(textField, BorderLayout.CENTER);
	
		browseButton = new JButton("Browse");
		browseButton.addActionListener( new BrowseHandler() );
		add(browseButton, BorderLayout.EAST );
		
		infoLabel = new JLabel(" ");
		add(infoLabel, BorderLayout.SOUTH );
		
		browseDialog = new JFileChooser();
		if (initialDir != null)
			browseDialog.setCurrentDirectory(initialDir);
		browseDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		textField.getDocument().addDocumentListener( new TextChangeHandler() );
	}
	
	public void setFileListener(FileLineListener listener)
	{
		this.listener = listener;
	}
	
	public File getFile()
	{
		return file;
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		
		textField.setEnabled(enabled);
		browseButton.setEnabled(enabled);
		
		nameLabel.setEnabled(enabled);
		infoLabel.setEnabled(enabled);
	}
	
	public void setText(String newText)
	{
		textField.setText(newText);
	}
	
	public void setInfo(String infoText)
	{
		ignoreTextEvents = true;
		
		infoLabel.setText(infoText);
		
		ignoreTextEvents = false;
	}
	
	private void onFileChanged()
	{
		if (listener != null)
			listener.onFileChanged(file);
	}
	
	private class BrowseHandler implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			final int result = browseDialog.showDialog(parent, browseLabel);
			if (result == JFileChooser.APPROVE_OPTION)
			{
				file = browseDialog.getSelectedFile();
			}
			else if (result == JFileChooser.CANCEL_OPTION)
			{
				
			}
			else if (result == JFileChooser.ERROR_OPTION)
			{
				
			}
			
			ignoreTextEvents = true;
			
			setText(file.getAbsolutePath());
			
			ignoreTextEvents = false;
			
			onFileChanged();
		}
	}
	
	private class TextChangeHandler implements DocumentListener
	{
		@Override
		public void changedUpdate(DocumentEvent e)
		{
			if (ignoreTextEvents)
				return;
			
			file = new File(textField.getText());
			
			onFileChanged();
		}
		
		@Override
		public void insertUpdate(DocumentEvent e)
		{
			if (ignoreTextEvents)
				return;
			
			file = new File(textField.getText());
			
			onFileChanged();
		}
		
		@Override
		public void removeUpdate(DocumentEvent e)
		{
			if (ignoreTextEvents)
				return;
			
			String text = textField.getText();
			file = new File(text);
			
			onFileChanged();
		}
	}
}
