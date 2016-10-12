package com.github.sbellus.fitnesse.xml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import fitnesse.html.HtmlTag;
import fitnesse.html.RawHtml;
import fitnesse.wikitext.parser.Matcher;
import fitnesse.wikitext.parser.Maybe;
import fitnesse.wikitext.parser.Parser;
import fitnesse.wikitext.parser.Rule;
import fitnesse.wikitext.parser.Symbol;
import fitnesse.wikitext.parser.SymbolType;
import fitnesse.wikitext.parser.Translation;
import fitnesse.wikitext.parser.Translator;

public class XmlSymbol extends SymbolType implements Rule, Translation {
    private String conversionXslt;
    private String conversionStyle;
    
    public static SymbolType make() {
        System.out.println("XML CREATING");
        return new XmlSymbol();
    }
    
    public XmlSymbol() {
        super("ListingXml");
        wikiMatcher(new Matcher().string("!listing_xm").endsWith(new char[] {'(', '{', '['}));
        wikiRule(this);
        htmlTranslation(this);
        
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            conversionXslt =  IOUtils.toString(classLoader.getResourceAsStream("Conversion.xslt"), "UTF-8");
        } catch (IOException e1) {
            conversionXslt = "";
        }
        try {
            conversionStyle = IOUtils.toString(classLoader.getResourceAsStream("Conversion.style"), "UTF-8");
        } catch (IOException e) {
            conversionStyle = "";
        }
    }

    public Maybe<Symbol> parse(Symbol current, Parser parser) {
        String content = current.getContent();
        char beginner = content.charAt(content.length() - 1);
        content = parser.parseLiteral(closeType(beginner));
        if (parser.atEnd())
        {
            return Symbol.nothing;
        }
        current.putProperty("xml", content);
        
        return new Maybe<Symbol>(current);
    }

    private static SymbolType closeType(char beginner) {
        return beginner == '[' ? SymbolType.CloseBracket
                : beginner == '{' ? SymbolType.CloseBrace : SymbolType.CloseParenthesis;
    }

    private String trim(String input) {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringBuffer result = new StringBuffer();
        try {
            String line;
            while ( (line = reader.readLine() ) != null)
                result.append(line.trim());
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public String toTarget(Translator translator, Symbol symbol) {
        String html = "";

        try {
                XMLReader reader = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
                InputStream xml = new ByteArrayInputStream(trim(symbol.getProperty("xml")).getBytes(StandardCharsets.UTF_8));
                Source input = new SAXSource(reader, new InputSource(xml));
                InputStream conversionXsltStream = new ByteArrayInputStream(conversionXslt.getBytes(StandardCharsets.UTF_8));
                Source xsl = new StreamSource(conversionXsltStream);
                Transformer transformer = TransformerFactory.newInstance().newTransformer(xsl);
                transformer.setParameter("indent-elements", "yes");
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                transformer.transform(input, new StreamResult(output));
                html = conversionStyle + "\n" + output.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        HtmlTag xmlSection = new HtmlTag("div");
        xmlSection.addAttribute("class", "listing_xml");
        String originalXml = trim(symbol.getProperty("xml"));
        originalXml = originalXml.replaceAll("\"", "&quot");
        originalXml = originalXml.replaceAll("'", "&amp");
        xmlSection.addAttribute("originalxml", originalXml);
        xmlSection.add(new RawHtml(html));

        return xmlSection.html();
    }
}
