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

func TestRunWorkflowError(t *testing.T) {
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
