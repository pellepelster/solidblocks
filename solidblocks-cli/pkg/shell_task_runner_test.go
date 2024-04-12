package pkg

import (
	"github.com/alecthomas/assert/v2"
	"os/user"
	"strings"
	"testing"
)

var logger = func(s string) {
	Outputln(s)
}

func TestShellTaskRunnerCommand(t *testing.T) {
	result := ShellTaskRunner{Command: []string{"whoami"}}.Run(make(map[string]string), logger)
	currentUser, err := user.Current()

	assert.NoError(t, err)
	assert.True(t, result.Success)
	assert.Equal(t, currentUser.Username, strings.TrimSpace(result.Output))
}

func TestShellTaskRunnerCommandArguments(t *testing.T) {
	result := ShellTaskRunner{Command: []string{"id", "-u"}}.Run(make(map[string]string), logger)
	currentUser, err := user.Current()

	assert.NoError(t, err)
	assert.True(t, result.Success)
	assert.Equal(t, currentUser.Uid, strings.TrimSpace(result.Output))
}

func TestShellTaskRunnerCommandFailure(t *testing.T) {
	result := ShellTaskRunner{Command: []string{"invalid_command"}}.Run(make(map[string]string), logger)
	assert.False(t, result.Success)
}

func TestShellTaskRunnerScript(t *testing.T) {
	result := ShellTaskRunner{Script: []string{"test.sh"}}.Run(make(map[string]string), logger)
	assert.True(t, result.Success)
}

func TestShellTaskRunnerScriptFailure(t *testing.T) {
	result := ShellTaskRunner{Script: []string{"invalid_script"}}.Run(make(map[string]string), logger)
	assert.False(t, result.Success)
}

func TestShellTaskRunnerParse(t *testing.T) {
	yml := `
shell:
  command: some_command
`
	runner, err := ParseTaskRunner(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, runner)
	assert.Zero(t, runner.(ShellTaskRunner).Workdir)
}

func TestShellTaskRunnerParseWorkdir(t *testing.T) {
	yml := `
shell:
  workdir: some_dir
  command: some_command
`
	runner, err := ParseTaskRunner(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, runner)
	assert.Equal(t, "some_dir", runner.(ShellTaskRunner).Workdir)
}
