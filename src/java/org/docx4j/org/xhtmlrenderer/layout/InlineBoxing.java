/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci, Torbjoern Gannholm
 * Copyright (c) 2005 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.docx4j.org.xhtmlrenderer.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.docx4j.org.xhtmlrenderer.css.constants.CSSName;
import org.docx4j.org.xhtmlrenderer.css.constants.IdentValue;
import org.docx4j.org.xhtmlrenderer.css.style.CalculatedStyle;
import org.docx4j.org.xhtmlrenderer.css.style.CssContext;
import org.docx4j.org.xhtmlrenderer.css.style.derived.BorderPropertySet;
import org.docx4j.org.xhtmlrenderer.css.style.derived.RectPropertySet;
import org.docx4j.org.xhtmlrenderer.render.AnonymousBlockBox;
import org.docx4j.org.xhtmlrenderer.render.BlockBox;
import org.docx4j.org.xhtmlrenderer.render.Box;
import org.docx4j.org.xhtmlrenderer.render.FSFontMetrics;
import org.docx4j.org.xhtmlrenderer.render.FloatDistances;
import org.docx4j.org.xhtmlrenderer.render.InlineBox;
import org.docx4j.org.xhtmlrenderer.render.InlineLayoutBox;
import org.docx4j.org.xhtmlrenderer.render.InlineText;
import org.docx4j.org.xhtmlrenderer.render.LineBox;
import org.docx4j.org.xhtmlrenderer.render.MarkerData;
import org.docx4j.org.xhtmlrenderer.render.StrutMetrics;
import org.docx4j.org.xhtmlrenderer.render.TextDecoration;

/**
 * This class is responsible for flowing inline content into lines.  Block
 * content which participates in an inline formatting context is also handled
 * here as well as floating and absolutely positioned content.
 */
public class InlineBoxing {
    
    private InlineBoxing() {
    }

    public static void layoutContent(LayoutContext c, BlockBox box, int initialY, int breakAtLine) {
        
        int maxAvailableWidth = 999;
        
        System.out.println("InlineBoxing.layoutContent" );
        
        CalculatedStyle parentStyle = box.getStyle();
        System.out.println( parentStyle.toStringMine() );
        

        for (Iterator i = box.getInlineContent().iterator(); i.hasNext(); ) {
                        
            Styleable node = (Styleable)i.next();

            // System.out.println( node.getElement().getLocalName() );
            if (node.getStyle().isInline()) {
                InlineBox iB = (InlineBox)node;

                CalculatedStyle style = iB.getStyle();
                if (iB.isStartsHere()) {
                    
                    if (node.getElement()==null) {
                        System.out.println(node.getClass().getName() + " has null element!" );                                                
                    } else {
                        System.out.println("<" +  node.getElement().getLocalName() + ".. isInline() .. " ); 
                        //System.out.println( node.getElement()..toStringMine() );                        
                        System.out.println( node.getStyle().toStringMine() );
                        
                    }

                    InlineLayoutBox currentIB = new InlineLayoutBox(c, iB.getElement(), style, maxAvailableWidth);
                    
                    // Handle children
                    positionHorizontally(c, currentIB);
                }


            } else {
                
                System.out.println(".. BlockBox()" );
                
               BlockBox child = (BlockBox)node;

               if (child.getStyle().isNonFlowContent()) {
                   System.out.println("encountered non flow content!" );                   
               } else if (child.getStyle().isInlineBlock() || child.getStyle().isInlineTable()) {
                   
                   layoutInlineBlockContent(c, box, child, initialY);

               }
            }
        }

    }

    private static void positionHorizontally(CssContext c, InlineLayoutBox current) {

        System.out.println("Processing InlineLayoutBox children, of which there are " + current.getInlineChildCount());
        
        for (int i = 0; i < current.getInlineChildCount(); i++) {
            Object child = current.getInlineChild(i);
            if (child instanceof InlineLayoutBox) {
                InlineLayoutBox iB = (InlineLayoutBox) child;
                // TODO - process
                System.out.println(".. child InlineLayoutBox");
                positionHorizontally(c, iB);
            } else if (child instanceof InlineText) {
                InlineText iT = (InlineText) child;
                // TODO - process
                System.out.println(".. child InlineText" + iT.getMasterText());
                System.out.println(".. child InlineText" + iT.getTextNode().getNodeValue() );
            } else if (child instanceof Box) {
                Box b = (Box) child;
                // TODO - process
                System.out.println(".. child Box .. TODO" );
            }
        }
    }   
    
    private static void layoutInlineBlockContent(
            LayoutContext c, BlockBox containingBlock, BlockBox inlineBlock, int initialY) {
        inlineBlock.setContainingBlock(containingBlock);
        inlineBlock.setContainingLayer(c.getLayer());
//        inlineBlock.initStaticPos(c, containingBlock, initialY);
//        inlineBlock.calcCanvasLocation();
        inlineBlock.layout(c);
    }    

    public static StrutMetrics createDefaultStrutMetrics(LayoutContext c, Box container) {
        FSFontMetrics strutM = container.getStyle().getFSFontMetrics(c);
        InlineBoxMeasurements measurements = getInitialMeasurements(c, container, strutM);

        return new StrutMetrics(
                strutM.getAscent(), measurements.getBaseline(), strutM.getDescent());
    }

    private static InlineBoxMeasurements getInitialMeasurements(
            LayoutContext c, Box container, FSFontMetrics strutM) {
        float lineHeight = container.getStyle().getLineHeight(c);

        int halfLeading = Math.round((lineHeight -
                container.getStyle().getFont(c).size) / 2);
        if (halfLeading > 0) {
            halfLeading = Math.round((lineHeight -
                    (strutM.getDescent() + strutM.getAscent())) / 2);
        }

        InlineBoxMeasurements measurements = new InlineBoxMeasurements();
        measurements.setBaseline((int) (halfLeading + strutM.getAscent()));
        measurements.setTextTop(halfLeading);
        measurements.setTextBottom((int) (measurements.getBaseline() + strutM.getDescent()));
        measurements.setInlineTop(halfLeading);
        measurements.setInlineBottom((int) (halfLeading + lineHeight));

        return measurements;
    }
    
}

