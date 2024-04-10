package pkg

import (
	"github.com/alecthomas/assert/v2"
	"os/user"
	"strings"
	"testing"
)

func TestShellTaskRunnerCommand(t *testing.T) {
	result := ShellTaskRunner{Command: []string{"whoami"}}.Run("task1", make(map[string]string))
	currentUser, err := user.Current()

	assert.NoError(t, err)
	assert.True(t, result.Success)
	assert.Equal(t, currentUser.Username, strings.TrimSpace(result.Output))
}

func TestShellTaskRunnerCommandArguments(t *testing.T) {
	result := ShellTaskRunner{Command: []string{"id", "-u"}}.Run("task1", make(map[string]string))
	currentUser, err := user.Current()

	assert.NoError(t, err)
	assert.True(t, result.Success)
	assert.Equal(t, currentUser.Uid, strings.TrimSpace(result.Output))
}

func TestShellTaskRunnerCommandFailure(t *testing.T) {
	result := ShellTaskRunner{Command: []string{"invalid_command"}}.Run("task1", make(map[string]string))
	assert.False(t, result.Success)
}

func TestShellTaskRunnerScript(t *testing.T) {
	result := ShellTaskRunner{Script: "test.sh"}.Run("task1", make(map[string]string))
	assert.True(t, result.Success)
}

func TestShellTaskRunnerScriptFailure(t *testing.T) {
	result := ShellTaskRunner{Script: "invalid_script"}.Run("task1", make(map[string]string))
	assert.False(t, result.Success)
}
