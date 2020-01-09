package uk.gov.caz.async.rest;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

/**
 * This test class can be used for manual tests. Comment "@Disabled" to enable tests. It should not
 * run in any automated CI  environments because it relies on network and external services so it is
 * ignored by default.
 */
@Disabled("Should not run automatically on CI/CD")
public class AsyncClientDemoTest {

  private interface GitHubApi {

    @GET("feeds")
    @Headers("Accept: application/json")
    Call<JsonNode> feeds();

    default Response<JsonNode> feedsSync() {
      try {
        return feeds().execute();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    default AsyncOp<JsonNode> feedsAsync() {
      return AsyncOp.from("GitHubFeedsOp", feeds());
    }

    @GET("orgs/{organization}/members")
    Call<JsonNode> orgMembers(@Path("organization") String organization);

    default AsyncOp<JsonNode> orgMembersAsync(String orgName) {
      return AsyncOp.from("GitHubOrgMembersOp", orgMembers(orgName));
    }

    default Response<JsonNode> orgMembersSync(String orgName) {
      try {
        return orgMembers(orgName).execute();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private interface TodoListApi {

    @GET("todos/{todoId}")
    Call<TodoItem> todoItem(@Path("todoId") int todoId);

    default AsyncOp<TodoItem> todoItemAsync(int todoId) {
      return AsyncOp.from("TodoItemOp", todoItem(todoId));
    }

    default Response<TodoItem> todoItemSync(int todoId) {
      try {
        return todoItem(todoId).execute();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @GET("todos/{todoId}")
    Call<JsonNode> todoItemAsJsonNode(@Path("todoId") int todoId);
  }

  @Data
  public static class TodoItem {

    int userId;
    int id;
    String title;
    boolean completed;
  }

  private GitHubApi gitHubApi;
  private TodoListApi todoListApi;
  private AsyncRestService asyncRestService;

  @BeforeEach
  public void setup() {
    gitHubApi = synthesizeGitHubApi();
    todoListApi = synthesizeTodoListApi();
    asyncRestService = new AsyncRestService();
  }

  @Test
  public void testSequentialCalls() {
    Stopwatch timer = Stopwatch.createStarted();

    Response<JsonNode> feedsResponse = gitHubApi.feedsSync();
    Response<JsonNode> orgMembersResponse = gitHubApi.orgMembersSync("patternmatch");
    Response<TodoItem> firstTodoResponse = todoListApi.todoItemSync(1);

    System.out.println(
        "Sequential test took (milliseconds): " + timer.stop().elapsed(TimeUnit.MILLISECONDS));

    assertThat(feedsResponse.code()).isEqualTo(200);
    System.out.println(feedsResponse.body());

    assertThat(orgMembersResponse.code()).isEqualTo(200);
    System.out.println(orgMembersResponse.body());

    assertThat(firstTodoResponse.code()).isEqualTo(200);
    System.out.println(firstTodoResponse.body());
  }

  @Test
  public void testAsyncCalls() {
    Stopwatch timer = Stopwatch.createStarted();

    AsyncOp<JsonNode> gitHubFeedsOp = gitHubApi.feedsAsync();
    AsyncOp<JsonNode> gitHubOrgMembersOp = gitHubApi.orgMembersAsync("patternmatch");
    AsyncOp<TodoItem> todoListFirstItemOp = todoListApi.todoItemAsync(1);

    asyncRestService
        .startAndAwaitAll(newArrayList(gitHubFeedsOp, gitHubOrgMembersOp, todoListFirstItemOp), 5,
            TimeUnit.SECONDS);

    System.out.println(
        "Async test took (milliseconds): " + timer.stop().elapsed(TimeUnit.MILLISECONDS));

    assertThat(gitHubFeedsOp.getHttpStatus()).isEqualTo(HttpStatus.OK);
    System.out.println(gitHubFeedsOp.getResult());

    assertThat(gitHubOrgMembersOp.getHttpStatus()).isEqualTo(HttpStatus.OK);
    System.out.println(gitHubOrgMembersOp.getResult());

    assertThat(todoListFirstItemOp.getHttpStatus()).isEqualTo(HttpStatus.OK);
    System.out.println(todoListFirstItemOp.getResult());
  }

  private Retrofit gitHubRetrofitAdapter() {
    return new Retrofit.Builder()
        .baseUrl("https://api.github.com")
        .addConverterFactory(jacksonConverterFactory())
        .build();
  }

  private Retrofit todoListApiRetrofitAdapter() {
    return new Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com")
        .addConverterFactory(jacksonConverterFactory())
        .build();
  }

  private JacksonConverterFactory jacksonConverterFactory() {
    return JacksonConverterFactory.create();
  }

  private GitHubApi synthesizeGitHubApi() {
    return gitHubRetrofitAdapter().create(GitHubApi.class);
  }

  private TodoListApi synthesizeTodoListApi() {
    return todoListApiRetrofitAdapter().create(TodoListApi.class);
  }
}
