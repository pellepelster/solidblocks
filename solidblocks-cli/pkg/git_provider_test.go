package pkg

import (
	"github.com/alecthomas/assert/v2"
	"os"
	"testing"
)

func TestGetGitInfo(t *testing.T) {
	path, err := os.Getwd()
	assert.NoError(t, err)

	_, err = GitGetInfo(path)
	assert.Error(t, err)
}
