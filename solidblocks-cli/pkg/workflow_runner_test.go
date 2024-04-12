package pkg

import (
	"github.com/alecthomas/assert/v2"
	"testing"
)

func TestRunWorkflow(t *testing.T) {
	workflowYaml := `
tasks:
  - task1:
      shell:
        command: whoami
  - task2:
      environment:
        - name: BLCKS_WHOAMI
          valueFrom:
            task: task1
      shell:
        command: ["id", "-u"]
`
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
	assert.Zero(t, err)
	assert.Equal(t, 2, len(workflow.Tasks))
	assert.Equal(t, "task1", workflow.Tasks[0].Name)
	assert.NotZero(t, workflow.Tasks[0].Runner)

	err = RunWorkflow(*workflow)
	assert.NoError(t, err)
}

func TestRunWorkflowDuplicateValueFrom(t *testing.T) {
	workflowYaml := `
tasks:
  - task1:
      shell:
        command: whoami
  - task2:
      environment:
        - name: VAR1
          valueFrom:
            task: task1
        - name: VAR2
          valueFrom:
            task: task1
      shell:
        command: ["id", "-u"]
`
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
	assert.Zero(t, err)
	assert.Equal(t, 2, len(workflow.Tasks))
	assert.Equal(t, "task1", workflow.Tasks[0].Name)
	assert.NotZero(t, workflow.Tasks[0].Runner)

	err = RunWorkflow(*workflow)
	assert.NoError(t, err)
}

func TestRunWorkflowErrorInvalidCommand(t *testing.T) {
	workflowYaml := `
tasks:
  - task1:
      shell:
        command: invalid_command
`
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
	assert.Zero(t, err)

	err = RunWorkflow(*workflow)
	assert.Error(t, err)
}

func disabled_TestRunWorkflowErrorInvalidSyntax(t *testing.T) {
	workflowYaml := `
tasks:
 : - &§ -4321
`
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
	assert.Zero(t, err)

	err = RunWorkflow(*workflow)
	assert.Error(t, err)
}

func TestRunWorkflowErrorInvalidValueFromTask(t *testing.T) {
	workflowYaml := `
tasks:
  - task1:
      environment:
        - name: VAR1
          valueFrom:
            task: task99
      shell:
        command: ["id", "-u"]
`
	workflow, err := WorkflowParse("workflow1", []byte(workflowYaml))
	assert.NotZero(t, workflow)
	assert.NoError(t, err)

	errors := WorkflowValidate(workflow)
	assert.Equal(t, 1, len(errors))
	assert.EqualError(t, errors[0], "environment variable 'VAR1' references task 'task99' which does not exist")
}
