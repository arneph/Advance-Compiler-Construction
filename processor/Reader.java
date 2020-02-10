package processor;

import java.io.*;

public class Reader {
	private File file;
	private FileReader reader;
	private StringBuilder contents;
	
	public char sym;

	public Reader(String fileName) {
		try {
			file = new File(fileName);
			reader = new FileReader(file);
			contents = new StringBuilder();
			
			int d = reader.read();
			if (d == -1) sym = 0xff;
			else sym = (char)d;
			contents.append((char)sym);
		} catch (IOException e) {
			e.printStackTrace();
			
			System.exit(0);
		}
	}
	
	public void Next() {
		try {
			int d = reader.read();
			if (d == -1) sym = 0xff;
			else sym = (char)d;
			contents.append((char)sym);
		} catch (IOException e) {
			e.printStackTrace();
			
			System.exit(0);
		}
	}
	
	public void Error(String errorMsg) {
		int c = contents.length() - 1;
		
		while (sym != 0xff && sym != '\n') {
			Next();
		}
		contents.setLength(contents.length() - 1);
		
		int s = contents.lastIndexOf("\n") + 1;
		
		System.err.println(errorMsg);
		System.err.println(contents.substring(s));
		for (int i = 0; i < c - s; i++) {
			System.err.print(' ');
		}
		System.err.println("^");
		System.exit(0);
	}
	
}
