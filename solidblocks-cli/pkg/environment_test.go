package pkg

import (
	"github.com/alecthomas/assert/v2"
	"testing"
)

func TestParseEnvironmentNameValue(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    value: "var1"
`
	envVars, err := ParseEnvironment(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, 1, len(envVars))
	assert.Equal(t, "VAR1", envVars[0].Name)
	assert.Equal(t, "var1", envVars[0].Value)
	assert.Zero(t, envVars[0].ValueFrom)
}

func TestParseEnvironmentNameOnly(t *testing.T) {
	yml := `
environment:
  - name: VAR1
`
	envVars, err := ParseEnvironment(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, 1, len(envVars))
	assert.Equal(t, "VAR1", envVars[0].Name)
	assert.Zero(t, envVars[0].Value)
	assert.Zero(t, envVars[0].ValueFrom)
}

func TestParseEnvironmentValueFromTask(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    valueFrom:
      task: task1
`
	envVars, err := ParseEnvironment(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, 1, len(envVars))
	assert.Equal(t, "VAR1", envVars[0].Name)
	assert.Zero(t, envVars[0].Value)
	assert.Equal(t, "task1", envVars[0].ValueFrom.Task)
}
