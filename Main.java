import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import model.dlx.*;
import model.ssa.*;
import model.syntax.*;

import processor.*;

import vcg.*;

public class Main {
	static String inFolderPath = "../Input/";
	static String outFolderPath = "../Output/";
	
	public static void main(String[] args) {
		File inFolder = new File(inFolderPath);
		List<File> inFiles = Arrays.asList(inFolder.listFiles());
		List<String> names = inFiles.stream()
				.map(File::getName)
				.filter(s -> s.endsWith(".txt"))
				.map(s -> s.substring(0, s.length() - 4))
				.collect(Collectors.toList());
		
		Collections.sort(names);
		
		for (String name : names) {
			compile(name);
		}
		
		System.out.println("Added move instructions: " + RegisterAllocator.moveInstrCount);
	}
	
	static void compile(String name) {
		System.out.printf("--- %-10s ---\n", name);
		
		String inFilePath = inFolderPath + name + ".txt";
		String outPath = outFolderPath + name + "/";
		
		File outFolder = new File(outPath);
		outFolder.mkdirs();
		
		// Output files:
		String outSyntaxPath = outPath + name + ".syntax.vcg";
		
		String outSSACFGPath = outPath + name + ".ssa.cfg.vcg";
		String outSSADomPath = outPath + name + ".ssa.dom.vcg";
		
		String outSSAGenTXTPath = outPath + name + ".ssa.gen.txt";
		String outSSAGenVCGPath = outPath + name + ".ssa.gen.vcg";
		
		String outDefUseGenPath = outPath + name + ".def-use.gen.txt";
		
		String outEliminationTrace = outPath + name + ".elimination.txt";
		
		String outSSAOptTXTPath = outPath + name + ".ssa.opt.txt";
		String outSSAOptVCGPath = outPath + name + ".ssa.opt.vcg";
		
		String outDefUseOptPath = outPath + name + ".def-use.opt.txt";
		
		String outLiveRangesTrace = outPath + name + ".liveranges.txt";
		String outRegAssignmentsTrace = outPath + name + ".regassignments.txt";
		String outColorsPath = outPath + name + ".colors.vcg";
		
		String outSSARegTXTPath = outPath + name + ".ssa.reg.txt";
		String outSSARegVCGPath = outPath + name + ".ssa.reg.vcg";
		
		String outDLXGenPath = outPath + name + ".dlx.gen.txt";

		String outDLXAsmPath = outPath + name + ".dlx.asm.txt";
		String outDLXBinPath = outPath + name + ".dlx.asm.bin";
		String outDLXBinTxtPath = outPath + name + ".dlx.asm.bin.txt";
		
		// Scanning & Parsing:
		Parser parser = new Parser(inFilePath);
		
		SymbolTable symbolTable = parser.getTable();
		Computation computation = parser.parse();
		
		VCGGraph syntaxTree = new VCGGraph(computation.getVCGNode().getConnectedNodes());
		writeToFile(syntaxTree.toString(), outSyntaxPath);
		
		// SSA Generator:
		SSAGenerator generator = new SSAGenerator(symbolTable, computation);
		
		Program ssaProgram = generator.generate();
		
		VCGGraph cfgBlocks = new VCGGraph(ssaProgram.getControlFlowVCGNodes());
		VCGGraph domBlocks = new VCGGraph(ssaProgram.getDominationVCGNodes());
		writeToFile(cfgBlocks.toString(), outSSACFGPath);
		writeToFile(domBlocks.toString(), outSSADomPath);

		writeToFile(ssaProgram.toString(), outSSAGenTXTPath);
		
		VCGGraph ssaGenBlocks = new VCGGraph(ssaProgram.getInstructionVCGNodes());
		writeToFile(ssaGenBlocks.toString(), outSSAGenVCGPath);
		
		// SSA Analyzer:
		SSAAnalyzer analyzer = new SSAAnalyzer(ssaProgram);
		
		DefUseTable defUseTable = analyzer.getDefUseTable();
		
		writeToFile(defUseTable.toString(), outDefUseGenPath);
		
		// SSA Optimizer:
		StringBuilder eliminationTrace = new StringBuilder();
		SSAOptimizer optimizer = new SSAOptimizer(ssaProgram, defUseTable, eliminationTrace);
		
		optimizer.removeMoveInstructions();
		//optimizer.removeUnusedInstructions();
		//optimizer.removeUnusedInstructions();
		optimizer.removeCommonSubexpressions();
		//optimizer.removeUnusedInstructions();
		
		writeToFile(defUseTable.toString(), outDefUseOptPath);
		writeToFile(eliminationTrace.toString(), outEliminationTrace);
		
		writeToFile(ssaProgram.toString(), outSSAOptTXTPath);
		
		VCGGraph ssaOptBlocks = new VCGGraph(ssaProgram.getInstructionVCGNodes());
		writeToFile(ssaOptBlocks.toString(), outSSAOptVCGPath);
		
		// Register Allocator:
		StringBuilder liveRangesTrace = new StringBuilder();
		StringBuilder regAssignmentsTrace = new StringBuilder();
		RegisterAllocator registerAllocator = new RegisterAllocator(ssaProgram, defUseTable, liveRangesTrace, regAssignmentsTrace);
		
		registerAllocator.allocate();
		
		writeToFile(liveRangesTrace.toString(), outLiveRangesTrace);
		writeToFile(regAssignmentsTrace.toString(), outRegAssignmentsTrace);
		
		VCGGraph regColorGraph = new VCGGraph(registerAllocator.getVCGNodes());
		writeToFile(regColorGraph.toString(), outColorsPath);
		
		writeToFile(ssaProgram.toString(), outSSARegTXTPath);
		
		VCGGraph ssaRegBlocks = new VCGGraph(ssaProgram.getInstructionVCGNodes());
		writeToFile(ssaRegBlocks.toString(), outSSARegVCGPath);
		
		// Memory Allocator:
		MemoryAllocator memoryAllocator = new MemoryAllocator(computation, symbolTable);
		
		memoryAllocator.allocate();
		
		// DLX Generator:
		DLXGenerator codeGenerator = new DLXGenerator(symbolTable, ssaProgram, memoryAllocator.getGlobalSpace());
		
		DLXProgram dlxProgram = codeGenerator.generate();
		
		writeToFile(dlxProgram.toString(), outDLXGenPath);
		
		// DLX Assembler:
		DLXAssembler codeAssembler = new DLXAssembler(dlxProgram);
		
		codeAssembler.resolveLabels();
		
		ArrayList<Integer> code = codeAssembler.generateCode();

		writeToFile(dlxProgram.toString(), outDLXAsmPath);
		writeToFile(code, outDLXBinPath, outDLXBinTxtPath);
	}
	
	private static void writeToFile(ArrayList<Integer> code, String binPath, String txtPath) {
		try {
			File binFile = new File(binPath);
			FileOutputStream outputStream = new FileOutputStream(binFile);
			
			File txtFile = new File(txtPath);
			FileWriter txtWriter = new FileWriter(txtFile);
			
			for (int instr : code) {
				txtWriter.write(intToBinary(instr));
				txtWriter.write("\n");
				
				byte[] bin = new byte[4];
				
				bin[0] = (byte)(instr >> 24);
				bin[1] = (byte)(instr >> 16);
				bin[2] = (byte)(instr >>  8);
				bin[3] = (byte)(instr >>  0);
				
				outputStream.write(bin);
			}
			
			outputStream.flush();
			outputStream.close();
			
			txtWriter.flush();
			txtWriter.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			
			System.exit(0);
		}
	}
	
	private static void writeToFile(String contents, String path) {
		try {
			File file = new File(path);
			FileWriter writer = new FileWriter(file);
			
			writer.write(contents);
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			
			System.exit(0);
		}
	}
	
	public static String intToBinary (int n) {
	   String binary = "";
	   for(int i = 31; i >= 0; i--) {
		   if (((n >> i) & 0x1) == 1) binary += "1";
		   else binary += "0";
	   }

	   return binary;
	}

}
