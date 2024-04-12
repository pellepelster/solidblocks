package pkg

import (
	"github.com/alecthomas/assert/v2"
	"testing"
)

func TestParseTaskVariable(t *testing.T) {
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

func TestParseTaskVariableValueFrom(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    valueFrom:
      task: task1
shell:
  command: some_command
`

	task, err := ParseTask("task1", PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, task)
	assert.Equal(t, 1, len(task.Environment))
	assert.NotZero(t, "VAR1", len(task.Environment[0].Name))
	assert.Zero(t, task.Environment[0].Value)
	assert.Equal(t, "task1", task.Environment[0].ValueFrom.Task)
}

func TestParseTaskVariableValueFromTransform(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    transform:
      json: "$.yolo"
    valueFrom:
      task: task1
shell:
  command: some_command
`

	task, err := ParseTask("task1", PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, task)
	assert.Equal(t, 1, len(task.Environment))
	assert.NotZero(t, "VAR1", len(task.Environment[0].Name))
	assert.Zero(t, task.Environment[0].Value)
	assert.Equal(t, "task1", task.Environment[0].ValueFrom.Task)
	assert.Equal(t, "$.yolo", task.Environment[0].Transform.Json)
}

func TestParseTaskWithDescription(t *testing.T) {
	yml := `
description: "some description"
shell:
  command: some_command
`

	task, err := ParseTask("task1", PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, task)
	assert.Equal(t, "some description", task.Description)
}
