package pkg

import (
	"errors"
	"fmt"
	"github.com/divideandconquer/go-merge/merge"
	"github.com/google/uuid"
	"reflect"
)

func RandomUUID() string {
	newUUID, err := uuid.NewRandom()
	if err != nil {
		panic(err)
	}

	return newUUID.String()
}

func GetStringKey(key string, data interface{}) (string, error) {
	if !IsMap(data) {
		return "", errors.New("is not a map")
	}

	value := data.(map[string]interface{})[key]

	if value == nil {
		return "", errors.New(fmt.Sprintf("key '%s' not found", key))
	}

	reflectValue := reflect.ValueOf(value)

	if reflectValue.Kind() == reflect.String {
		return reflectValue.String(), nil
	}

	return "", errors.New(fmt.Sprintf("value for key '%s' is not a string", key))
}

func GetSliceKey(key string, data interface{}) ([]interface{}, error) {
	if !IsMap(data) {
		return nil, errors.New("is not a map")
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
		return nil, errors.New("is not a map")
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

func GetKeyAndData(data interface{}) (string, map[string]interface{}, error) {
	if !IsMap(data) {
		return "", nil, errors.New("is not a map")
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
		return "", nil, errors.New("map is not nested")
	}

	return key.String(), value.Elem().Interface().(map[string]interface{}), nil
}

func MergeDataByKey(keys []string, data interface{}) (map[string]interface{}, error) {
	if !IsMap(data) {
		return nil, errors.New("is not a map")
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
		return nil, errors.New("is not a map")
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
