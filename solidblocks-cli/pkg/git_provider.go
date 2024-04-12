package pkg

import (
	"github.com/go-git/go-git/v5"
)

type GitInfo struct {
	Hash string
}

func GitGetInfo(path string) (*GitInfo, error) {
	r, err := git.PlainOpen(path)
	if err != nil {
		return nil, err
	}

	head, err := r.Head()
	if err != nil {
		return nil, err
	}

	return &GitInfo{Hash: head.Hash().String()}, nil
}
