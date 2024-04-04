package pkg

import (
	"errors"
	"fmt"
	"os/exec"
	"strings"
)

func PassHasSecret(passwordStoreDir, secret string) bool {
	secret, err := PassGetSecret(passwordStoreDir, secret)
	return err == nil && len(secret) > 0
}

func PassGetSecret(passwordStoreDir, secret string) (string, error) {
	cmd := exec.Command("pass", []string{secret}...)
	cmd.Env = []string{fmt.Sprintf("%s=%s", "PASSWORD_STORE_DIR", passwordStoreDir)}
	out, err := cmd.CombinedOutput()
	if err != nil {
		return "", errors.New(fmt.Sprintf("secret '%s' not found in secret store at '%s'", secret, passwordStoreDir))
	}

	return strings.TrimSpace(string(out)), nil
}
