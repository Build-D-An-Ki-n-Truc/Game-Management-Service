package com.highman;

import com.google.gson.Gson;
import com.highman.models.DBConnectionPool;
import com.highman.prometheus.MetricsRegisters;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import grpc.*;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.all;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public class GameManagementService extends GameManagementServiceGrpc.GameManagementServiceImplBase {
    MongoClient mongoClient;
    MongoCollection<Document> gameColl;
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(GameManagementService.class);

    public GameManagementService() {
        try {
            mongoClient = MongoClients.create(System.getenv("MONGO_URI"));
            MongoDatabase mongoDatabase = mongoClient.getDatabase("game_service");
            gameColl = mongoDatabase.getCollection("games");

//            printOneDocument("669a8089bf71b13349b55968");
            printAllDocuments();
            MetricsRegisters.requests.inc();
        } catch (Exception e) {
            String error = "An error has occured while retrieving database connection: " + e.getMessage();
            e.printStackTrace();
            LOGGER.debug(error);
        }
    }

    // UPDATE
    @Override
    public void updateInfo(GameManagementInfoRequest request, StreamObserver<GameManagementResponse> responseObserver) {
        GameManagementResponse.Builder response = GameManagementResponse.newBuilder();

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);

            // Find the game by its id and update its info
            Publisher<UpdateResult> publisher = gameColl.updateOne(
                    eq("_id", new ObjectId(request.getId())),
                    combine(
                            set("name", request.getName()),
                            set("image", request.getImage()),
                            set("type", request.getType()),
                            set("allowedItemTrade", request.getAllowedItemTrade()),
                            set("tutorial", request.getTutorial()),
                            set("startTime", new Date(request.getStartTime())),
                            set("endTime", new Date(request.getEndTime())),
                            set("config.maxPlayers", request.getMaxPlayers()),
                            set("config.duration", request.getDuration())
                    )
            );

            // Perform update
            Mono.from(publisher)
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(
                            updateResult -> {
                                String msg = "Game info update complete.";
                                System.out.println(msg);

                                response.setFinished(true);
                                response.setMessage(msg);
                                responseObserver.onNext(response.build());
                                responseObserver.onCompleted();
                            },
                            throwable -> {
                                String msg = "Failed to update document: " + throwable;
                                System.err.println(msg);
                                throwable.printStackTrace();

                                response.setFinished(false);
                                response.setMessage(msg);
                                responseObserver.onNext(response.build());
                                responseObserver.onCompleted();
                            }
                    );
        } catch (Exception e) {
            // Error message
            String msg = "Error while updating game info" + e.getMessage();
            System.err.println(msg);
            e.printStackTrace();

            response.setFinished(false);
            response.setMessage(msg);
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
    }

    // UPDATE
    @Override
    public void updateStatus(GameManagementStatusRequest request, StreamObserver<GameManagementResponse> responseObserver) {
        GameManagementResponse.Builder response = GameManagementResponse.newBuilder();

        try {
            // Find the game by its id and update its info
            Publisher<UpdateResult> publisher = gameColl.updateOne(
                    eq("_id", new ObjectId(request.getId())),
                    combine(
                            set("status", request.getStatus())
                    )
            );

            // Perform update
            Mono.from(publisher)
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(
                            updateResult -> {
                                String msg = "Game status update complete.";
                                System.out.println(msg);

                                response.setFinished(true);
                                response.setMessage(msg);
                                responseObserver.onNext(response.build());
                                responseObserver.onCompleted();
                            },
                            throwable -> {
                                String msg = "Failed to update document: " + throwable;
                                System.err.println(msg);
                                throwable.printStackTrace();

                                response.setFinished(false);
                                response.setMessage(msg);
                                responseObserver.onNext(response.build());
                                responseObserver.onCompleted();
                            }
                    );
        } catch (Exception e) {
            // Error message
            String msg = "Error while updating game status" + e.getMessage();
            System.err.println(msg);
            e.printStackTrace();

            response.setFinished(false);
            response.setMessage(msg);
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
    }

    // READ
    @Override
    public void getAll(GameManagementGetAllRequest request, StreamObserver<GameManagementGetAllResponse> responseObserver) {
        // Response builder
        GameManagementGetAllResponse.Builder response = GameManagementGetAllResponse.newBuilder();
        // Query
        Flux.from(gameColl.find(new Document()))
                .publishOn(Schedulers.boundedElastic())
                .collectList()
                .subscribe(
                        documents -> {
                            // Every document holds the data of a game
                            for (Document document : documents) {
                                // Retrieve basic info of this game...
                                String id = Objects.toString(document.get("_id"), "");
                                String name = Objects.toString(document.get("name"), "");
                                String type = Objects.toString(document.get("type"), "");
                                Boolean allowedItemTrade = (Boolean) document.get("allowedItemTrade");
                                String tutorial = Objects.toString(document.get("tutorial"), "");
                                String status = Objects.toString(document.get("status"), "");
                                long startTime = ((Date) document.get("startTime")).getTime();
                                long endTime = ((Date) document.get("endTime")).getTime();

                                Document config = document.get("config", new Document());
                                Integer maxPlayers = config.getInteger("maxPlayers");
                                Integer duration = config.getInteger("duration");

                                //...and store it in the builder (single game)
                                GameManagementGetResponse.Builder getResponseBuilder = GameManagementGetResponse.newBuilder()
                                        .setId(id)
                                        .setName(name)
                                        .setType(type)
                                        .setAllowedItemTrade(allowedItemTrade)
                                        .setTutorial(tutorial)
                                        .setStatus(status)
                                        .setStartTime(startTime)
                                        .setEndTime(endTime)
                                        .setMaxPlayers(maxPlayers)
                                        .setDuration(duration);

                                // Get all data on questions of this game and store it in the single-game builder
                                List<Document> questions = document.getList("questions", Document.class);
                                for (Document question: questions) {
                                    GameManagementQuestion.Builder questionBuilder = GameManagementQuestion.newBuilder();
                                    questionBuilder.setText(question.getString("text"))
                                            .setCorrectAnswer(question.getInteger("correctAnswer"))
                                            .addAllOptions(question.getList("options", String.class))
                                            .build();

                                    getResponseBuilder.addQuestions(questionBuilder);
                                }

                                // Add the single-game builder to the main builder
                                response.addGames(getResponseBuilder.build());
                            }

                            // Return response
                            responseObserver.onNext(response.build());
                            responseObserver.onCompleted();
                        },
                        throwable -> {
                            System.out.println("Error: " + throwable);
                            throwable.printStackTrace();

                            // Return empty response if err
                            responseObserver.onNext(response.build());
                            responseObserver.onCompleted();
                        }
                );
    }

    // Debug
    private void printAllDocuments() {
        Flux.from(gameColl.find(new Document()))
                .publishOn(Schedulers.boundedElastic())
                .collectList()
                .subscribe(
                        documents -> {
                            System.out.printf("| %-30s | %-30s | %-10s | %-30s | %-30s | %-10s | %-50s | %-50s | %-15s | %-15s |\n", "id", "name", "type", "allowedItemTrade", "tutorial", "status", "startTime", "endTime", "maxPlayers", "duration");
                            for (Document document : documents) {
                                String id = Objects.toString(document.get("_id"), "");
                                String name = Objects.toString(document.get("name"), "");
                                String type = Objects.toString(document.get("type"), "");
                                String allowedItemTrade = Objects.toString(document.get("allowedItemTrade"), "");
                                String tutorial = Objects.toString(document.get("tutorial"), "");
                                String status = Objects.toString(document.get("status"), "");
                                String startTime = Objects.toString(document.get("startTime"), "");
                                String endTime = Objects.toString(document.get("endTime"), "");
                                Document config = document.get("config", new Document());
                                String maxPlayers = Objects.toString(config.get("maxPlayers"), "");
                                String duration = Objects.toString(config.get("duration"), "");
                                System.out.printf("| %-30s | %-30s | %-10s | %-30s | %-30s | %-10s | %-50s | %-50s | %-15s | %-15s |\n", id, name, type, allowedItemTrade, tutorial, status, startTime, endTime, maxPlayers, duration);
                            }
                        },
                        throwable -> System.out.println("Error: " + throwable)
                );
    }
    private void printOneDocument(String documentId) {
        Flux.from(gameColl.find(new Document()))
                .publishOn(Schedulers.boundedElastic())
                .collectList()
                .subscribe(
                        documents -> {
                            System.out.printf("| %-30s | %-30s | %-10s | %-30s | %-30s | %-10s | %-50s | %-50s | %-15s | %-15s |\n", "id", "name", "type", "allowedItemTrade", "tutorial", "status", "startTime", "endTime", "maxPlayers", "duration");
                            for (Document document : documents) {
                                if (Objects.equals(document.get("_id", ""), documentId)) {
                                    String name = Objects.toString(document.get("name"), "");
                                    String type = Objects.toString(document.get("type"), "");
                                    String allowedItemTrade = Objects.toString(document.get("allowedItemTrade"), "");
                                    String tutorial = Objects.toString(document.get("tutorial"), "");
                                    String status = Objects.toString(document.get("status"), "");
                                    String startTime = Objects.toString(document.get("startTime"), "");
                                    String endTime = Objects.toString(document.get("endTime"), "");
                                    Document config = document.get("config", new Document());
                                    String maxPlayers = Objects.toString(config.get("maxPlayers"), "");
                                    String duration = Objects.toString(config.get("duration"), "");
                                    System.out.printf("| %-30s | %-30s | %-10s | %-30s | %-30s | %-10s | %-50s | %-50s | %-15s | %-15s |\n", documentId, name, type, allowedItemTrade, tutorial, status, startTime, endTime, maxPlayers, duration);

                                    break;
                                }
                            }
                        },
                        throwable -> System.out.println("Error: " + throwable)
                );
    }
}
