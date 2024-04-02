package pkg

import (
	"github.com/alecthomas/assert/v2"
	"testing"
)

func TestEmptyWorkflow(t *testing.T) {
	workflow, err := ParseWorkflow("workflow1", []byte(""))
	assert.Zero(t, err)
	assert.NotZero(t, workflow)
	assert.Equal(t, 0, len(workflow.Tasks))
}

func TestWorkflowEnvironmentVariableGlobal(t *testing.T) {
	workflowYaml := `
environment:
  - name: BLCKS_GLOBAL1
    value: "blcks_global1"
`
	workflow, err := ParseWorkflow("workflow1", []byte(workflowYaml))
	assert.NoError(t, err)
	assert.Equal(t, 1, len(workflow.Environment))
	assert.Equal(t, "BLCKS_GLOBAL1", workflow.Environment[0].Name)
	assert.Equal(t, "blcks_global1", workflow.Environment[0].Value)
}

func TestWorkflowEnvironmentVariableInherited(t *testing.T) {
	workflowYaml := `
environment:
  - name: BLCKS_INHERITED1
`
	workflow, err := ParseWorkflow("workflow1", []byte(workflowYaml))
	assert.NoError(t, err)
	assert.Equal(t, 1, len(workflow.Environment))
	assert.Equal(t, "BLCKS_INHERITED1", workflow.Environment[0].Name)
	assert.Zero(t, workflow.Environment[0].Value)
}

func TestWorkflowShellStepNoCommand(t *testing.T) {
	workflowYaml := `
tasks:
  - task1:
      shell:
`
	_, err := ParseWorkflow("workflow1", []byte(workflowYaml))
	assert.Error(t, err)
	assert.Equal(t, "task1: either a command or a script has to be provided", err.Error())
}

func TestWorkflowHasTask(t *testing.T) {
	workflowYaml := `
tasks:
  - task1:
      shell:
        command: whoami
`
	workflow, err := ParseWorkflow("workflow1", []byte(workflowYaml))
	assert.NoError(t, err)
	assert.True(t, workflow.hasTask("task1"))
	assert.False(t, workflow.hasTask("task2"))
}

func TestWorkflowGetTask(t *testing.T) {
	workflowYaml := `
tasks:
  - task1:
      shell:
        command: whoami
`
	workflow, err := ParseWorkflow("workflow1", []byte(workflowYaml))
	assert.NoError(t, err)
	assert.NotZero(t, workflow.getTask("task1"))
	assert.Zero(t, workflow.getTask("task2"))
}

func TestWorkflowShellStepInvalidRunner(t *testing.T) {
	workflowYaml := `
tasks:
  - task1:
      invalid:
`
	_, err := ParseWorkflow("workflow1", []byte(workflowYaml))
	assert.Error(t, err)
	assert.Equal(t, "task1: runner 'invalid' not found", err.Error())
}

func TestWorkflowInvalidStep(t *testing.T) {
	workflowYaml := `
tasks:
  - task1: invalid
`
	workflow, err := ParseWorkflow("workflow1", []byte(workflowYaml))
	assert.Error(t, err)
	assert.Zero(t, workflow)
}
