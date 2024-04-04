package pkg

import (
	"github.com/alecthomas/assert/v2"
	"testing"
)

func TestParseTaskRunner(t *testing.T) {
	yml := `
shell:
  command: some_command
`
	taskRunner, err := ParseTaskRunner(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, taskRunner)
}

func TestParseTaskRunnerVariables(t *testing.T) {
	yml := `
shell:
  command: some_command
`

	taskRunner, err := ParseTaskRunner(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, taskRunner)
}

func TestParseTaskVariables(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    value: "var1"
shell:
  command: some_command
`

	task, err := ParseTask("task1", PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, task)
	assert.Equal(t, 1, len(task.Environment))
	assert.NotZero(t, "VAR1", len(task.Environment[0].Name))
	assert.NotZero(t, "var1", len(task.Environment[0].Value))
}
