package pkg

import (
	"errors"
	"fmt"
	"github.com/divideandconquer/go-merge/merge"
	"github.com/fatih/color"
	"github.com/google/uuid"
	"github.com/urfave/cli/v2"
	"golang.org/x/term"
	"os/exec"
	"reflect"
	"strings"
)

func RandomUUID() string {
	newUUID, err := uuid.NewRandom()
	if err != nil {
		panic(err)
	}

	return newUUID.String()
}

func GetStringKey(key string, data interface{}) (string, error) {
	value := GetOptionalStringKey(key, data)

	if len(value) == 0 {
		return "", errors.New(fmt.Sprintf("no value found for key '%s'", key))
	}

	return value, nil
}

func GetOptionalStringKey(key string, data interface{}) string {
	if !IsMap(data) {
		return ""
	}

	value := data.(map[string]interface{})[key]
	reflectValue := reflect.ValueOf(value)

	if reflectValue.Kind() == reflect.String {
		return reflectValue.String()
	}

	return ""
}

func GetOptionalObjectKey(key string, data interface{}) map[string]interface{} {
	if !IsMap(data) {
		return nil
	}

	value := data.(map[string]interface{})[key]
	reflectValue := reflect.ValueOf(value)

	if reflectValue.Kind() == reflect.Map {
		return value.(map[string]interface{})
	}

	return nil
}

func GetSliceKey(key string, data interface{}) ([]interface{}, error) {
	if !IsMap(data) {
		return nil, errors.New("invalid format, not an object")
	}

	value := data.(map[string]interface{})[key]

	if value == nil {
		return nil, errors.New(fmt.Sprintf("key '%s' not found", key))
	}

	if IsSlice(value) {
		return value.([]interface{}), nil
	}

	return nil, errors.New(fmt.Sprintf("value for key '%s' is not a slice", key))
}

func IsSlice(data interface{}) bool {
	return reflect.TypeOf(data).Kind() == reflect.Slice
}

func IsMap(data interface{}) bool {
	return reflect.TypeOf(data).Kind() == reflect.Map
}

func GetAsStringList(key string, data interface{}) ([]string, error) {
	if !IsMap(data) {
		return nil, errors.New("invalid format, not an object")
	}

	value := data.(map[string]interface{})[key]
	if value == nil {
		return nil, errors.New("key '%s' not found")
	}

	reflectValue := reflect.ValueOf(value)

	if reflectValue.Kind() == reflect.String {
		return []string{reflectValue.String()}, nil
	}

	if reflectValue.Kind() == reflect.Slice {

		values := make([]string, 0)

		for i := 0; i < reflectValue.Len(); i++ {
			element := reflectValue.Index(i)
			values = append(values, element.Elem().String())
		}

		return values, nil
	}

	return []string{}, nil
}

func GetAsString(key string, data interface{}) (string, error) {
	if !IsMap(data) {
		return "", errors.New("invalid format, not an object")
	}

	value := data.(map[string]interface{})[key]
	if value == nil {
		return "", errors.New("key '%s' not found")
	}

	reflectValue := reflect.ValueOf(value)

	if reflectValue.Kind() == reflect.String {
		return reflectValue.String(), nil
	}

	return "", nil
}

func GetKeyAndData(data interface{}) (string, map[string]interface{}, error) {
	if !IsMap(data) {
		return "", nil, errors.New("invalid format, not an object")
	}

	mapRange := reflect.ValueOf(data).MapRange()

	if !mapRange.Next() {
		return "", nil, errors.New("map is empty")
	}

	key := mapRange.Key()
	value := mapRange.Value()

	if value.IsNil() {
		return key.String(), nil, nil
	}

	if key.Kind() != reflect.String || value.Elem().Kind() == reflect.String || value.Elem().Kind() == reflect.Bool || value.Elem().Kind() == reflect.Uint64 {
		return key.String(), nil, errors.New("invalid format, not an object")
	}

	return key.String(), value.Elem().Interface().(map[string]interface{}), nil
}

func GetDataForKey(key string, data interface{}) (map[string]interface{}, error) {
	if !IsMap(data) {
		return nil, errors.New("invalid format, not an object")
	}

	for mapKey, mapValue := range data.(map[string]interface{}) {
		if mapKey == key {
			if reflect.ValueOf(mapValue).Kind() == reflect.Map {
				return mapValue.(map[string]interface{}), nil
			} else {
				return make(map[string]interface{}), nil
			}
		}
	}

	return nil, nil
}

func MergeDataByKey(keys []string, data interface{}) (map[string]interface{}, error) {
	if !IsMap(data) {
		return nil, errors.New("invalid format, not an object")
	}

	return nil, nil
}

func CollectDataByKey(rootKey, key string, data interface{}) (map[string]interface{}, error) {
	return collectDataByKeyAndName(rootKey, key, "", data)
}

func CollectDataByKeyAndName(rootKey, key, name string, data interface{}) (map[string]interface{}, error) {
	return collectDataByKeyAndName(rootKey, key, name, data)
}

func collectDataByKeyAndName(rootKey, key, name string, data interface{}) (map[string]interface{}, error) {

	if !IsMap(data) {
		return nil, errors.New("invalid format, not an object")
	}

	mapData := data.(map[string]interface{})

	if mapData[rootKey] == nil {
		return nil, nil
	}

	if !IsSlice(mapData[rootKey]) {
		return nil, errors.New(fmt.Sprintf("key '%s' is not list", key))
	}

	listData := mapData[rootKey].([]interface{})
	for _, element := range listData {
		dataKey, data, err := GetKeyAndData(element)
		if err != nil {
			return nil, err
		}

		if key == dataKey && (len(name) == 0 || data["name"] == name) {
			return data, nil
		}
	}

	return nil, nil
}

func MergeMaps(base, override map[string]interface{}) map[string]interface{} {
	return merge.Merge(base, override).(map[string]interface{})
}

func MergeStringMaps(base, override map[string]string) map[string]string {
	return merge.Merge(base, override).(map[string]string)
}

func CommandExists(command string) bool {
	_, err := exec.Command("which", command).CombinedOutput()
	return err == nil
}

func Unique(s []string) []string {
	unique := make(map[string]bool, len(s))
	us := make([]string, len(unique))
	for _, elem := range s {
		if len(elem) != 0 {
			if !unique[elem] {
				us = append(us, elem)
				unique[elem] = true
			}
		}
	}

	return us
}

func Output(s string) {
	println(s)
}

func Outputf(s string, a ...any) {
	Output(fmt.Sprintf(s, a...))
}

func OutputDebugf(context *cli.Context, s string, a ...any) {
	if context.Bool("debug") {
		Output(fmt.Sprintf(s, a...))
	}
}

func OutputTaskf(prefix, s string, a ...any) {
	Output(prefix + ": " + fmt.Sprintf(s, a...))
}

func OutputDividerTask(prefix, divider string) {
	OutputTaskf(prefix, strings.Repeat(divider, TermWidth()-len(prefix)-2))
}

func OutputDivider(divider string) {
	Output(strings.Repeat(divider, TermWidth()))
}

func Reverse[S ~[]E, E any](s S) {
	for i, j := 0, len(s)-1; i < j; i, j = i+1, j-1 {
		s[i], s[j] = s[j], s[i]
	}
}

func TermWidth() int {
	if !term.IsTerminal(0) {
		return 80
	}
	width, _, err := term.GetSize(0)
	if err != nil {
		return 80
	}
	return width
}

var TextBoldBlack = color.New(color.FgHiBlack, color.Bold).SprintFunc()
var TextPrimary = color.New(color.FgCyan, color.Bold).SprintFunc()
var TextSecondary = color.New(color.FgYellow).SprintFunc()
var TextAlert = color.New(color.BgHiRed, color.FgBlack, color.Bold).SprintFunc()
var TextSuccess = color.New(color.FgGreen).SprintFunc()
