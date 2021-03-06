package io.takari.bpm.xml.activiti;

import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.SubProcess;
import io.takari.bpm.xml.Parser;
import java.io.InputStream;
import static org.junit.Assert.*;
import org.junit.Test;

public class ActivitiParserTest {

    @Test
    public void testComplex() throws Exception {
        InputStream in = ClassLoader.getSystemResourceAsStream("complex.bpmn");
        Parser p = new ActivitiParser();

        ProcessDefinition pd = p.parse(in);
        assertNotNull(pd);
        assertEquals("cpaChargingReserve", pd.getId());
    }
    
    @Test
    public void testMixed() throws Exception {
        InputStream in = ClassLoader.getSystemResourceAsStream("mixed.bpmn");
        Parser p = new ActivitiParser();

        ProcessDefinition pd = p.parse(in);
        assertNotNull(pd);
        
        SequenceFlow f = (SequenceFlow) pd.getChild("flow27");
        assertNotNull(f);
        assertEquals("${spaStatus == 'A'}", f.getExpression());
    }
    
    @Test
    public void testComplex2() throws Exception {
        InputStream in = ClassLoader.getSystemResourceAsStream("complex2.bpmn");
        Parser p = new ActivitiParser();

        ProcessDefinition pd = p.parse(in);
        assertNotNull(pd);
        
        SubProcess sp1 = (SubProcess) pd.getChild("reserveSubprocess");
        assertNotNull(sp1);
        assertEquals(7, sp1.getChildren().size());
        
        SubProcess sp2 = (SubProcess) pd.getChild("subprocess1");
        assertNotNull(sp2);
        assertEquals(32, sp2.getChildren().size());
    }
}
