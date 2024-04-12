package pkg

import (
	"github.com/alecthomas/assert/v2"
	"os/user"
	"testing"
)

func TestEnvironmentValue(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    value: "var1"
`
	vars, err := EnvironmentParse(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, vars[0].Value)
	assert.Zero(t, vars[0].ValueFrom)
	assert.Zero(t, vars[0].Transform)
	assert.Zero(t, vars[0].Default)

	assert.Equal(t, 1, len(vars))
	assert.Equal(t, "VAR1", vars[0].Name)
	assert.Equal(t, "var1", vars[0].Value)

	value, err := EnvironmentResolve(vars[0])
	assert.Equal(t, "var1", value)
}

func TestEnvironmentValueAndValueFrom(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    value: "var1"
    valueFrom:
      command: whoami
`
	_, err := EnvironmentParse(PrepareTestData(t, yml))
	assert.Error(t, err)
	assert.EqualError(t, err, "variable VAR1: 'value' and 'valueFrom' must not be set at the same time")
}

func TestEnvironmentValueFromCommand(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    valueFrom:
      command: whoami
`
	vars, err := EnvironmentParse(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Zero(t, vars[0].Value)
	assert.NotZero(t, vars[0].ValueFrom)
	assert.NotZero(t, vars[0].ValueFrom.Command)
	assert.Zero(t, vars[0].Transform)
	assert.Zero(t, vars[0].Default)

	assert.Equal(t, 1, len(vars))
	assert.Equal(t, "VAR1", vars[0].Name)

	currentUser, err := user.Current()
	assert.NoError(t, err)

	value, err := EnvironmentResolve(vars[0])
	assert.Equal(t, currentUser.Username, value)
}

func TestEnvironmentNameOnlyIsSet(t *testing.T) {
	yml := `
environment:
  - name: USERNAME
`
	vars, err := EnvironmentParse(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Zero(t, vars[0].Value)
	assert.Zero(t, vars[0].ValueFrom)
	assert.Zero(t, vars[0].Transform)
	assert.Zero(t, vars[0].Default)

	assert.Equal(t, 1, len(vars))
	assert.Equal(t, "USERNAME", vars[0].Name)

	currentUser, err := user.Current()
	assert.NoError(t, err)
	value, err := EnvironmentResolve(vars[0])
	assert.Equal(t, currentUser.Username, value)
}

func TestEnvironmentNameOnlyNoSet(t *testing.T) {
	yml := `
environment:
  - name: VAR1
`
	vars, err := EnvironmentParse(PrepareTestData(t, yml))

	assert.NoError(t, err)

	value, err := EnvironmentResolve(vars[0])
	assert.Equal(t, "", value)
}

func TestEnvironmentDefaultValueString(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    default: "var1"
`
	vars, err := EnvironmentParse(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Zero(t, vars[0].Value)
	assert.Zero(t, vars[0].ValueFrom)
	assert.Zero(t, vars[0].Transform)
	assert.NotZero(t, vars[0].Default)
	assert.Zero(t, vars[0].Default.ValueFrom)
	assert.NotZero(t, vars[0].Default.Value)

	assert.Equal(t, 1, len(vars))
	assert.Equal(t, "VAR1", vars[0].Name)
	assert.Equal(t, "var1", vars[0].Default.Value)

	value, err := EnvironmentResolve(vars[0])
	assert.Equal(t, "var1", value)
}

func TestEnvironmentDefaultValueObject(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    default:
      value: "var1"
`
	vars, err := EnvironmentParse(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Zero(t, vars[0].Value)
	assert.Zero(t, vars[0].ValueFrom)
	assert.Zero(t, vars[0].Transform)
	assert.NotZero(t, vars[0].Default)
	assert.Zero(t, vars[0].Default.ValueFrom)
	assert.NotZero(t, vars[0].Default.Value)

	assert.Equal(t, 1, len(vars))
	assert.Equal(t, "VAR1", vars[0].Name)
	assert.Equal(t, "var1", vars[0].Default.Value)

	value, err := EnvironmentResolve(vars[0])
	assert.Equal(t, "var1", value)
}

func TestEnvironmentDescription(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    description: "some description"
`
	envVars, err := EnvironmentParse(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, 1, len(envVars))
	assert.Equal(t, "some description", envVars[0].Description)
}

func TestParseEnvironmentValueFromTask(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    valueFrom:
      task: task1
`
	envVars, err := EnvironmentParse(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Zero(t, envVars[0].Value)
	assert.NotZero(t, envVars[0].ValueFrom)
	assert.NotZero(t, envVars[0].ValueFrom.Task)
	assert.Zero(t, envVars[0].Transform)
	assert.Zero(t, envVars[0].Default)

	assert.Equal(t, 1, len(envVars))
	assert.Equal(t, "VAR1", envVars[0].Name)
	assert.Zero(t, envVars[0].Value)
	assert.Equal(t, "task1", envVars[0].ValueFrom.Task)
}

func TestParseEnvironmentDefaultValueFromTask(t *testing.T) {
	yml := `
environment:
  - name: VAR1
    default:
      valueFrom:
        task: task1
`
	envVars, err := EnvironmentParse(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Zero(t, envVars[0].Value)
	assert.Zero(t, envVars[0].ValueFrom)
	assert.Zero(t, envVars[0].Transform)
	assert.NotZero(t, envVars[0].Default)
	assert.Zero(t, envVars[0].Default.Value)
	assert.NotZero(t, envVars[0].Default.ValueFrom)
	assert.NotZero(t, envVars[0].Default.ValueFrom.Task)

	assert.Equal(t, 1, len(envVars))
	assert.Equal(t, "VAR1", envVars[0].Name)
	assert.Zero(t, envVars[0].Value)
	assert.Equal(t, "task1", envVars[0].Default.ValueFrom.Task)
}
