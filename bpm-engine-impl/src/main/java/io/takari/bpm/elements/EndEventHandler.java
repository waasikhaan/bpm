package io.takari.bpm.elements;

import java.util.List;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.state.BpmnErrorHelper;
import io.takari.bpm.state.ProcessInstance;

public class EndEventHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        EndEvent ev = (EndEvent) ProcessDefinitionUtils.findElement(pd, cmd.getElementId());
        String errorRef = ev.getErrorRef();
        String causeVariable = ev.getCauseVariable();
        if (errorRef != null) {

            Throwable cause;
            if (causeVariable != null) {
                cause = (Throwable) state.getVariables().getVariable(causeVariable);
            } else {
                cause = null;
            }

            actions.add(BpmnErrorHelper.raiseError(errorRef, cause));
        }

        return actions;
    }
}
