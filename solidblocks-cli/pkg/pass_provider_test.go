package pkg

import (
	"fmt"
	"github.com/alecthomas/assert/v2"
	"os"
	"testing"
)

func TestPassHasSecret(t *testing.T) {
	path, err := os.Getwd()
	assert.NoError(t, err)
	assert.True(t, PassHasSecret(fmt.Sprintf("%s/../test/pass1", path), "some/secret1"))
}

func TestPassGetSecret(t *testing.T) {
	path, err := os.Getwd()
	assert.NoError(t, err)
	secret, err := PassGetSecret(fmt.Sprintf("%s/../test/pass1", path), "some/secret1")
	assert.NoError(t, err)
	assert.Equal(t, "foo bar", secret)
}

func TestPassGetSecretInvalid(t *testing.T) {
	path, err := os.Getwd()
	assert.NoError(t, err)
	secret, err := PassGetSecret(fmt.Sprintf("%s/../test/pass1", path), "invalid")
	assert.Error(t, err)
	assert.EqualError(t, err, fmt.Sprintf("secret 'invalid' not found in secret store at '%s/../test/pass1'", path))
	assert.Zero(t, secret)
}

func TestPassHasSecretInvalid(t *testing.T) {
	path, err := os.Getwd()
	assert.NoError(t, err)
	assert.False(t, PassHasSecret(fmt.Sprintf("%s/../test/pass1", path), "invalid/secret"))
}
