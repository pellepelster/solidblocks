package s3

import (
	"context"
	"errors"
	"fmt"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/service/s3/types"
	"github.com/spf13/cobra"
	"log"
)

var Name string

var Region string

var BackendS3Cmd = &cobra.Command{
	Use:   "s3",
	Short: "Initialize terraform S3 storage backend",

	RunE: func(cmd *cobra.Command, args []string) error {

		cfg, err := config.LoadDefaultConfig(context.TODO(),
			config.WithRegion(Region))
		if err != nil {
			log.Fatalln(err)
			return err
		}

		client := s3.NewFromConfig(cfg)

		exists, err := BucketExists(client, Name)

		if exists {
			log.Printf("bucket '%s' already exists", Name)
			return nil
		}

		log.Printf("creating bucket '%s'", Name)

		_, err = client.CreateBucket(context.TODO(), &s3.CreateBucketInput{
			Bucket: &Name,
			CreateBucketConfiguration: &types.CreateBucketConfiguration{
				LocationConstraint: types.BucketLocationConstraint(Region),
			},
		})
		if err != nil {
			log.Fatalln(err)
			return err
		}

		fmt.Printf("created bucket '%s'", Name)

		return nil
	},
}

func init() {
	BackendS3Cmd.Flags().StringVarP(&Name, "name", "n", "", "name of the S3 bucket")
	BackendS3Cmd.MarkFlagRequired("name")

	BackendS3Cmd.Flags().StringVarP(&Region, "region", "r", "", "name of AWS region")
	BackendS3Cmd.MarkFlagRequired("region")
}

func BucketExists(client *s3.Client, bucketName string) (bool, error) {

	_, err := client.HeadBucket(context.TODO(), &s3.HeadBucketInput{
		Bucket: aws.String(bucketName),
	})

	exists := true

	if err != nil {
		var apiError smithy.APIError

		if errors.As(err, &apiError) {
			switch apiError.(type) {
			case *types.NotFound:
				exists = false
				err = nil
			default:
				log.Printf("could not get bucket status for bucker '%s' (%s)", bucketName, err)
			}
		}
	}

	return exists, err
}
