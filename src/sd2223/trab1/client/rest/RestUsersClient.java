package sd2223.trab1.client.rest;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.core.GenericType;
import sd2223.trab1.api.User;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Users;
import sd2223.trab1.api.rest.UsersService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


public class RestUsersClient extends RestClient implements Users {

	final WebTarget target;
	
	public RestUsersClient( URI serverURI ) {
		super( serverURI );
		target = client.target( serverURI ).path( UsersService.PATH );
	}
	
	private Result<String> clt_createUser( User user) {
		
		Response r = target.request()
				.post(Entity.entity(user, MediaType.APPLICATION_JSON));

		return super.toJavaResult(r, String.class);
	}
	
	private Result<User> clt_getUser(String name, String pwd) {

		Response r = target.path( name )
				.queryParam(UsersService.PWD, pwd).request()
				.accept(MediaType.APPLICATION_JSON)
				.get();

		return super.toJavaResult(r, User.class);
	}

	private Result<User> clt_updateUser(String userId, String password, User user) {
		Response r = target.path( userId )
				.queryParam(UsersService.PWD, password).request()
				.put(Entity.entity(user, MediaType.APPLICATION_JSON));

		return super.toJavaResult(r, User.class);
	}

	private Result<User> clt_deleteUser(String userId, String password) {
		Response r = target.path( userId )
				.queryParam(UsersService.PWD, password).request()
				.delete();

		return super.toJavaResult(r, User.class);
	}

	private Result<List<User>> clt_searchUsers(String pattern) {
		Response r = target.queryParam(UsersService.QUERY, pattern).request()
				.accept(MediaType.APPLICATION_JSON).get();

		return super.toJavaResult(r, new GenericType<List<User>>() {});
	}
	
	@Override
	public Result<String> createUser(User user) {
		return super.reTry(() -> clt_createUser(user));
	}

	@Override
	public Result<User> getUser(String name, String pwd) {
		return super.reTry(() -> clt_getUser(name, pwd));
	}

	@Override
	public Result<User> updateUser(String userId, String password, User user) {
		return super.reTry(() -> clt_updateUser(userId, password, user));
	}

	@Override
	public Result<User> deleteUser(String userId, String password) {
		return super.reTry(() -> clt_deleteUser(userId, password));
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		return super.reTry(() -> clt_searchUsers(pattern));
	}
}
