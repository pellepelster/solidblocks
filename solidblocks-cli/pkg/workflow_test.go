package pkg

import (
	"github.com/alecthomas/assert/v2"
	"testing"
)

func TestEmptyWorkflow(t *testing.T) {
	workflow, err := WorkflowParse("workflow1", []byte(""))
	assert.Zero(t, err)
	assert.NotZero(t, workflow)
	assert.Equal(t, 0, len(workflow.Tasks))
}

func TestWorkflowEnvVarsGlobal(t *testing.T) {
	workflowYaml := `
environment:
  - name: VAR1
    value: "var1"
`
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
	assert.NoError(t, err)
	assert.Equal(t, 1, len(workflow.Environment))
	assert.Equal(t, "VAR1", workflow.Environment[0].Name)
	assert.Equal(t, "var1", workflow.Environment[0].Value)
}

func TestWorkflowEnvVarsGlobalDuplicate(t *testing.T) {
	workflowYaml := `
environment:
  - name: VAR1
    value: "var1"
  - name: VAR1
    value: "var1"
`
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
	assert.NoError(t, err)
	assert.Equal(t, 2, len(workflow.Environment))
	assert.Equal(t, "VAR1", workflow.Environment[0].Name)
	assert.Equal(t, "var1", workflow.Environment[0].Value)
	assert.Equal(t, "VAR1", workflow.Environment[1].Name)
	assert.Equal(t, "var1", workflow.Environment[1].Value)

	validationErrors := WorkflowValidate(workflow)
	assert.Equal(t, 1, len(validationErrors))
	assert.EqualError(t, validationErrors[0], "environment variable 'VAR1' defined more than once")
}

func TestWorkflowValidateTaskInvalidValueFrom(t *testing.T) {
	workflowYaml := `
tasks:
  - task1:
      environment:
        - name: VAR1
          valueFrom:
            task: task2
      shell:
        command: whoami
`
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
	assert.NoError(t, err)

	validationErrors := WorkflowValidate(workflow)
	assert.Equal(t, 1, len(validationErrors))
	assert.EqualError(t, validationErrors[0], "environment variable 'VAR1' references task 'task2' which does not exist")
}

func TestWorkflowEnvironmentVariableInherited(t *testing.T) {
	workflowYaml := `
environment:
  - name: BLCKS_INHERITED1
`
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
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
	_, err := WorkflowParse("workflow1", []byte(workflowYaml))
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
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
	assert.NoError(t, err)
	assert.True(t, workflow.HasTask("task1"))
	assert.False(t, workflow.HasTask("task2"))
}

func TestWorkflowGetTask(t *testing.T) {
	workflowYaml := `
tasks:
  - task1:
      shell:
        command: whoami
`
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
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
	_, err := WorkflowParse("workflow1", []byte(workflowYaml))
	assert.Error(t, err)
	assert.Equal(t, "task1: no runner found", err.Error())
}

func TestWorkflowInvalidStep(t *testing.T) {
	workflowYaml := `
tasks:
  - task1: invalid
`
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
	assert.Error(t, err)
	assert.EqualError(t, err, "task1: invalid format, not an object")
	assert.Zero(t, workflow)
}
