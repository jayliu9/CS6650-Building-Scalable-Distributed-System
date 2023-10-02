/*
 * Album Store API
 *
 * CS6650 Fall 2023
 *
 * API version: 1.0.0
 * Contact: i.gorton@northeasern.edu
 * Generated by: Swagger Codegen (https://github.com/swagger-api/swagger-codegen.git)
 */
package swagger

import (
	"net/http"
	"encoding/json"
    "io/ioutil"
    "github.com/gorilla/mux"
    "fmt"
)

var dao *AlbumDAO

func init() {
    dao = NewAlbumDAO()
}

func GetAlbumByKey(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)

    albumId, ok := vars["albumID"]
    if !ok {
        writeErrorResponse(w, http.StatusBadRequest, "missing parameters")
        return
    }

    albumInfo := dao.GetAlbumByKey(albumId)
    
	respData, err := json.Marshal(albumInfo)
	if err != nil {
		writeErrorResponse(w, http.StatusInternalServerError, "Error marshalling JSON")
		return
	}
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(http.StatusOK)
    w.Write(respData)
}


func writeErrorResponse(w http.ResponseWriter, statusCode int, message string) {
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(statusCode)
	resp := map[string]string{
		"error": message,
	}
	respData, _ := json.Marshal(resp)
	w.Write(respData)
}

func NewAlbum(w http.ResponseWriter, r *http.Request) {
    // Parse form data to retrieve file parts
    err := r.ParseMultipartForm(10 << 20) // 10MB is the max size of form data.
    if err != nil {
        http.Error(w, "Unable to parse form", http.StatusBadRequest)
        return
    }

    // Get image part and its size
    imagePart, _, err := r.FormFile("image")
    if err != nil {
        http.Error(w, "Unable to get image", http.StatusBadRequest)
        return
    }
    defer imagePart.Close()
    imageData, _ := ioutil.ReadAll(imagePart)

    // Get profile part and read its content
    profilePart := r.FormValue("profile")
    if profilePart == "" {
        http.Error(w, "Unable to get profile", http.StatusBadRequest)
        return
    }
    newAlbum := AlbumInfo{}
    err = json.Unmarshal([]byte(profilePart), &newAlbum)
    if err != nil {
        http.Error(w, "Error processing profile data", http.StatusBadRequest)
        return
    }

    albumPostResponse := map[string]interface{}{
        "ID":       "newAlbumId",
        "ImageSize" : fmt.Sprintf("%d", len(imageData)),
    }

    respData, err := json.Marshal(albumPostResponse)
    if err != nil {
        http.Error(w, "Error converting response to JSON", http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "application/json; charset=UTF-8")
    w.WriteHeader(http.StatusOK)
    w.Write(respData)
}
