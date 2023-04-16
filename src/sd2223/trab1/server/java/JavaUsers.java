package sd2223.trab1.server.java;

import sd2223.trab1.api.User;
import sd2223.trab1.api.java.Users;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Result.ErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class JavaUsers implements Users {
	private final Map<String, User> users = new HashMap<>();

	private static final Logger Log = Logger.getLogger(JavaUsers.class.getName());

	@Override
	public Result<String> createUser(User user) {
		Log.info("createUser : " + user);
		
		// Check if user data is valid
		if(user.getName() == null || user.getPwd() == null || user.getDisplayName() == null || user.getDomain() == null) {
			Log.info("User object invalid.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		// Insert user, checking if name already exists
		if( users.putIfAbsent(user.getName(), user) != null ) {
			Log.info("User already exists.");
			return Result.error( ErrorCode.CONFLICT);
		}
		return Result.ok(user.getName().concat("@" + user.getDomain()));
	}

	@Override
	public Result<User> getUser(String name, String pwd) {
		Log.info("getUser : user = " + name + "; pwd = " + pwd);
		
		// Check if user is valid
		if(name == null || pwd == null) {
			Log.info("Name or Password null.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}
		
		User user = users.get(name);			
		// Check if user exists 
		if( user == null ) {
			Log.info("User does not exist.");
			return Result.error(ErrorCode.NOT_FOUND);
		}
		
		//Check if the password is correct
		if( !user.getPwd().equals( pwd)) {
			Log.info("Password is incorrect.");
			return Result.error(ErrorCode.FORBIDDEN);
		}
		
		return Result.ok(user);
	}

	@Override
	public Result<User> updateUser(String name, String pwd, User user) {
		Log.info("updateUser : user = " + name + "; pwd = " + pwd + " ; user = " + user);

		Result<Void> result = verifyPassword(name, pwd); // All necessary checks are done in this method
		if(!result.isOK())
			return Result.error(result.error());

		if (!user.getName().equals(name)) // Name cannot be updatedn
			return Result.error(ErrorCode.BAD_REQUEST);

		// Only update the fields that are not null, name and domain cannot be updated
		User u = users.computeIfPresent(name, (key, userToUpdate) -> { // PROVAVELMENTE PODE SE USAR APENAS COMPUTE
			if (user.getPwd() != null)
				userToUpdate.setPwd(user.getPwd());
			if (user.getDisplayName() != null)
				userToUpdate.setDisplayName(user.getDisplayName());
			return userToUpdate;
		});
		return Result.ok(u);

	}

	@Override
	public Result<User> deleteUser(String name, String pwd) {
		Log.info("deleteUser : user = " + name + "; pwd = " + pwd);

		Result<Void> result = verifyPassword(name, pwd); // All necessary checks are done in this method
		if(!result.isOK())
			return Result.error(result.error());

		return Result.ok(users.remove(name));
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Log.info("searchUsers : pattern = " + pattern);

		if (pattern == null) {
			Log.info("Pattern null.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}
		if (pattern.equals("")) {
			return Result.ok((List<User>) users.values());
		}

		List<User> list = new ArrayList<>();

		for (Map.Entry<String, User> e :users.entrySet()) {
			if (e.getKey().matches("(?i).*" + pattern + ".*"))
				list.add(e.getValue());
		}

		return Result.ok(list);
	}

	@Override
	public Result<Void> verifyPassword(String name, String pwd) {
		var res = getUser(name, pwd);
		if( res.isOK() )
			return Result.ok();
		else
			return Result.error( res.error() );
	}
}
