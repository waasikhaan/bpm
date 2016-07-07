package io.takari.bpm.handlers;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.EventMapHelper;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.PersistExecutionCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.commands.SuspendExecutionCommand;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.event.Event;
import io.takari.bpm.model.IntermediateCatchEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import java.util.Date;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * Intermediate event handler. Its job is to create the child execution, suspend
 * it and link it with the event.
 */
public class IntermediateCatchEventHandler extends AbstractElementHandler {
    
    public static Date parseIso8601(String s) {
        return DateTime.parse(s).toDate();
    }

    public IntermediateCatchEventHandler(AbstractEngine engine) {
        super(engine);
    }

    @Override
    public void handle(DefaultExecution s, ProcessElementCommand c) throws ExecutionException {
        s.pop();
        
        Event e = makeEvent(c, s);
        
        IndexedProcessDefinition pd = getProcessDefinition(c);

        if (c.getGroupId() != null) {
            // grouped event
            SequenceFlow next = ProcessDefinitionUtils.findOutgoingFlow(pd, c.getElementId());
            
            EventMapHelper.put(s, e,
                    new PersistExecutionCommand(),
                    new ProcessElementCommand(pd.getId(), next.getId(), c.getGroupId(), c.isExclusive()));
        } else {
            // standalone event
            SequenceFlow next = ProcessDefinitionUtils.findOutgoingFlow(pd, c.getElementId());
            s.push(new ProcessElementCommand(pd.getId(), next.getId(), c.getGroupId(), c.isExclusive()));
            
            s.push(new SuspendExecutionCommand());
        }
        
        getEngine().getEventManager().add(e);
    }

    private Event makeEvent(ProcessElementCommand c, DefaultExecution s) throws ExecutionException {
        ProcessDefinition pd = getProcessDefinition(c);
        IntermediateCatchEvent ice = (IntermediateCatchEvent) ProcessDefinitionUtils.findElement(pd, c.getElementId());
        
        UUID id = getEngine().getUuidGenerator().generate();
        String name = getEventName(ice);
        
        ExpressionManager em = getEngine().getExpressionManager();
        ExecutionContext ctx = s.getContext();
        Date timeDate = parseTimeDate(ice.getTimeDate(), c, ctx, em);
        String timeDuration = eval(ice.getTimeDuration(), ctx, em, String.class);
        Date expiredAt = timeDate != null ? timeDate : parseDuration(timeDuration);
        
        return new Event(id, s.getId(), c.getGroupId(), name, s.getBusinessKey(), c.isExclusive(), expiredAt);
    }
    
    private Date parseTimeDate(String s, ProcessElementCommand c, ExecutionContext ctx, ExpressionManager em) throws ExecutionException {
        Object v = eval(s, ctx, em, Object.class);
        if (v == null) {
            return null;
        }
        
        if (v instanceof String) {
            return parseIso8601(s);
        } else if (v instanceof Date) {
            return (Date) v;
        } else {
            throw new ExecutionException("Invalid timeDate format in process '%s' in element '%s': '%s'", c.getProcessDefinitionId(), c.getElementId(), s);
        }
    }

    public static Date parseDuration(String s) throws ExecutionException {
        if(s == null) {
            return null;
        }

        if (isDuration(s)) {
            return DateTime.now().plus(Period.parse(s)).toDate();
        } else {
            throw new ExecutionException("Invalid duration format: '%s'", s);
        }
    }

    private static boolean isDuration(String time) {
        return time.startsWith("P");
    }
    
    private static String getEventName(IntermediateCatchEvent e) {
        return e.getMessageRef() != null ? e.getMessageRef() : e.getId();
    }

    private <T> T eval(String expr, ExecutionContext ctx, ExpressionManager em, Class<T> type) {
        if (expr == null || expr.trim().isEmpty()) {
            return null;
        }
        return em.eval(ctx, expr, type);
    }
}
