/*
 * (c) Copyright 2002, 2003, Hewlett-Packard Development Company, LP
 * [See end of file]
 */

package jena ;

import java.io.* ;
import jena.cmdline.*;

import java.util.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.FileUtils ;
import com.hp.hpl.jena.n3.* ;

/**
    Read N3 files and print in a variery of formats.
 * @author		Andy Seaborne
 * @version 	$Id: n3.java,v 1.12 2004-07-06 13:42:42 andy_seaborne Exp $
 */
public class n3
{
	static boolean firstOutput  = true ;
	static boolean doNodeTest   = true ;
	static boolean doErrorTests = false ;
	
	static int testCount = 0 ;	
	
	static boolean doRDF = false ;		// Attempt to create RDF
	static boolean printRDF = false ;	// List RDF
    static String  outputLang = "N-TRIPLE" ;
	static boolean printN3 = true ;		// List the N3 processed
    static boolean debug = false ;		// Help!
	static boolean verbose = false ;
	
	//static final String NL = JenaRuntime.getLineSeparator();

	// Parse a file (no RDF production)

	public static void main(String[] args)
	{
		String dir = System.getProperty("user.dir") ;
		
		String usageMessage = n3.class.getName()+
                                " [-rdf] [-base URI] [filename]" ;

    	CommandLine cmd = new CommandLine() ;
    	cmd.setUsage(usageMessage) ;
    	cmd.setOutput(System.err) ;
    	
    	//cmd.setHook(cmd.trace()) ;
    	
    	ArgDecl verboseDecl       = new ArgDecl(false, "-v", "--verbose") ;
    	ArgDecl helpDecl          = new ArgDecl(false, "-h", "--help") ;
    	ArgDecl rdfDecl           = new ArgDecl(false, "-rdf", "--rdf") ;
    	ArgDecl rdfRDFN3Decl      = new ArgDecl(false, "--rdf-n3") ;
    	ArgDecl rdfRDFXMLDecl     = new ArgDecl(false, "--rdf-xml") ;
    	ArgDecl rdfRDFNTDecl      = new ArgDecl(false, "--rdf-nt") ;
        ArgDecl rdfRDFFormat      = new ArgDecl(true, "--format", "--fmt") ;
    	ArgDecl debugDecl         = new ArgDecl(false, "-debug") ;
    	ArgDecl baseDecl          = new ArgDecl(true, "-base") ;
    	//ArgDecl outputDecl        = new ArgDecl(true, "-output", "-o") ;
    	ArgDecl checkDecl         = new ArgDecl(false, "-n", "--check") ;
    	
		cmd.add(verboseDecl) ;
		cmd.add(helpDecl) ;
		cmd.add(rdfDecl) ;
		cmd.add(rdfRDFN3Decl) ;
		cmd.add(rdfRDFXMLDecl) ;
		cmd.add(rdfRDFNTDecl) ;
        cmd.add(rdfRDFFormat) ;
		cmd.add(debugDecl) ;
		cmd.add(baseDecl) ;
		cmd.add(checkDecl) ;
		
		try { cmd.process(args) ; }
		catch (IllegalArgumentException illEx) { System.exit(1) ; }
    	
    	verbose = cmd.contains(verboseDecl) ;
    	
    	if ( cmd.contains(helpDecl) )
    	{
    		System.out.println(usageMessage) ;
    		System.out.println("Default action: parse an N3 file") ;
    		System.out.println("    --rdf           Read into an RDF and print") ;
    		System.out.println("    --rdf-n3        Read into an RDF and print in N3") ;
    		System.out.println("    --rdf-xml       Read into an RDF and print in XML") ;
    		System.out.println("    --rdf-nt        Read into an RDF and print in N-Triples") ;
            System.out.println("    --format FMT    Read into an RDF and print in given format") ;
    		System.out.println("    --check | -n    Just check: no output") ;
    		System.out.println("    --base URI      Set the base URI") ;
    		System.exit(0) ;
    	}
    	
		String baseName = null ;
    	
    	if ( cmd.contains(rdfDecl) )
    	{
    		doRDF =	true ;
    		printRDF = true ;
    		printN3 = false ;
    	}
    	
    	if ( cmd.contains(rdfRDFN3Decl) )
    	{
    		doRDF =	true ;
    		printRDF = true ;
    		outputLang = "N3" ;
    		printN3 = false ;
    	}

    	if ( cmd.contains(rdfRDFXMLDecl) )
    	{
    		doRDF =	true ;
    		printRDF = true ;
    		outputLang = "RDF/XML-ABBREV" ;
    		printN3 = false ;
    	}

    	if ( cmd.contains(rdfRDFNTDecl) )
    	{
    		doRDF =	true ;
    		printRDF = true ;
    		outputLang = "N-TRIPLE" ;
    		printN3 = false ;
    	}
    	
        if ( cmd.contains(rdfRDFFormat))
        {
            doRDF = true ;
            printRDF = true ;
            outputLang = cmd.getArg(rdfRDFFormat).getValue() ;
            printN3 = false ;
        }

    	if ( cmd.contains(debugDecl) )
    	{
    		debug = true ;
    		N3JenaWriter.DEBUG = true ;
    	}
    		
		if ( cmd.contains(checkDecl) )
		{
			printRDF = false ;
			printN3 = false ;
		}

		if ( cmd.contains(verboseDecl) )
		{
			verbose = true ;
			printN3 = true ;
		}
			
		if ( cmd.contains(baseDecl) )
			baseName = cmd.getArg(baseDecl).getValue() ;
			
			
		// stdin

		if ( cmd.items().size() == 0 )
		{
			if ( baseName == null )
				baseName = "stdin:/" ;
			doOneFile(System.in, System.out, baseName, baseName) ;
			System.exit(0) ;
		}
		
		// file arguments

		for ( Iterator iter = cmd.items().iterator() ; iter.hasNext() ; )
		{
			String filename = (String)iter.next() ;
			InputStream in = null ;
			try {
				// DO NOT use a FileReader : it gets default charset
				in = new FileInputStream(filename) ;
			} catch (FileNotFoundException noEx)
			{
				System.err.println("File not found: "+filename) ;
				System.exit(2) ;
			}
			if ( baseName == null )
			{
				File f = new File(filename) ;
				baseName = "file:///"+f.getAbsolutePath() ;
				baseName = baseName.replace('\\', '/') ;
			}
			
			doOneFile(in, System.out, baseName, filename) ;
		}
	}
	
	
	static void doOneFile(InputStream input, OutputStream output, String baseName, String filename)
	{
		BufferedReader reader = FileUtils.asBufferedUTF8(input) ;
		PrintWriter writer = FileUtils.asPrintWriterUTF8(output) ;

        if ( doRDF )
			rdfOneFile(reader, writer, baseName, filename) ;
		else
			parseOneFile(reader, writer, baseName, filename) ;
	}
	
	static void rdfOneFile(Reader reader, PrintWriter writer, String baseName, String filename)
	{
		try
		{
			Model model = ModelFactory.createDefaultModel();
			//RDFReader n3Reader = new N3JenaReader();
			//n3Reader.read(model, reader, baseName);
			model.read(reader, baseName, "N3") ;

			if (printRDF)
			{
				if ( outputLang.equals("N3") )
				{
					writer.print("# Jena N3->RDF->"+outputLang+" : " + filename);
					writer.println() ;
					writer.println() ;
				}
				//RDFWriter w = new N3JenaWriter();
				//w.write(model, writer, baseName);
				model.write(writer, outputLang, baseName) ;
			writer.flush();
			}
		} catch (JenaException rdfEx)
		{
            Throwable cause = rdfEx.getCause();
            N3Exception n3Ex = (N3Exception)
                (rdfEx instanceof N3Exception ? rdfEx
                : cause instanceof N3Exception ? cause
                : null);
            if ( n3Ex != null )
                System.err.println(n3Ex.getMessage()) ;
            else
            {
                Throwable th = (cause == null ? rdfEx : cause);
                System.err.println( th.getMessage() ) ;
                th.printStackTrace( System.err );
            }
            System.exit(7) ;
        }
	}
	
		
	static private void parseOneFile(Reader reader, PrintWriter writer, String baseName, String filename)
	{
		N3ParserEventHandler handler = null ;
		
		handler = null ;

		if ( printN3 || debug )
		{
			//out.println("# N3: "+filename) ;
			N3EventPrinter p = new N3EventPrinter(writer) ;
			if ( verbose )
				p.printStartFinish = true ;
			handler = p ;
		}
		else
			handler = new N3ErrorPrinter(writer) ;
		
		try {
			N3Parser n3Parser = new N3Parser(reader, handler) ;
			n3Parser.parse() ;
		} catch (antlr.RecognitionException ex)
		{
			//System.err.println(ex.getMessage()) ;
			//System.err.println("--------") ;			
			//System.err.println("Exception: "+ex) ;
			//ex.printStackTrace(System.err) ;
			//System.err.println("--------") ;			
			System.exit(9) ;
		}
		catch ( antlr.TokenStreamException tokEx)
		{
			System.exit(9) ;
		}
	}
}

/*
 *  (c) Copyright 2002, 2003 Hewlett-Packard Development Company, LP
 *  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
