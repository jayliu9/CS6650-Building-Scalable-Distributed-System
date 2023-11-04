/*
 * Album Store API
 * CS6650 Fall 2023
 *
 * OpenAPI spec version: 1.0.0
 * Contact: i.gorton@northeasern.edu
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * ImageMetaData
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2023-10-26T20:57:06.919122049Z[GMT]")

public class ImageMetaData {
  @SerializedName("albumID")
  private String albumID = null;

  @SerializedName("imageSize")
  private String imageSize = null;

  public ImageMetaData albumID(String albumID) {
    this.albumID = albumID;
    return this;
  }

   /**
   * Get albumID
   * @return albumID
  **/
  @Schema(description = "")
  public String getAlbumID() {
    return albumID;
  }

  public void setAlbumID(String albumID) {
    this.albumID = albumID;
  }

  public ImageMetaData imageSize(String imageSize) {
    this.imageSize = imageSize;
    return this;
  }

   /**
   * Get imageSize
   * @return imageSize
  **/
  @Schema(description = "")
  public String getImageSize() {
    return imageSize;
  }

  public void setImageSize(String imageSize) {
    this.imageSize = imageSize;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImageMetaData imageMetaData = (ImageMetaData) o;
    return Objects.equals(this.albumID, imageMetaData.albumID) &&
        Objects.equals(this.imageSize, imageMetaData.imageSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(albumID, imageSize);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImageMetaData {\n");
    
    sb.append("    albumID: ").append(toIndentedString(albumID)).append("\n");
    sb.append("    imageSize: ").append(toIndentedString(imageSize)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
