// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: service.proto
// Protobuf Java Version: 4.27.2

package grpc;

public interface GameManagementGetAllResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:grpc.GameManagementGetAllResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>repeated .grpc.GameManagementGetResponse games = 1;</code>
   */
  java.util.List<grpc.GameManagementGetResponse> 
      getGamesList();
  /**
   * <code>repeated .grpc.GameManagementGetResponse games = 1;</code>
   */
  grpc.GameManagementGetResponse getGames(int index);
  /**
   * <code>repeated .grpc.GameManagementGetResponse games = 1;</code>
   */
  int getGamesCount();
  /**
   * <code>repeated .grpc.GameManagementGetResponse games = 1;</code>
   */
  java.util.List<? extends grpc.GameManagementGetResponseOrBuilder> 
      getGamesOrBuilderList();
  /**
   * <code>repeated .grpc.GameManagementGetResponse games = 1;</code>
   */
  grpc.GameManagementGetResponseOrBuilder getGamesOrBuilder(
      int index);
}
