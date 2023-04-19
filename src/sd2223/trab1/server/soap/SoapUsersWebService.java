package sd2223.trab1.server.soap;


import java.util.List;
import java.util.logging.Logger;

import sd2223.trab1.api.User;
import sd2223.trab1.api.java.Users;
import sd2223.trab1.api.soap.UsersException;
import sd2223.trab1.api.soap.UsersService;
import sd2223.trab1.server.java.JavaUsers;
import jakarta.jws.WebService;

@WebService(serviceName=UsersService.NAME, targetNamespace=UsersService.NAMESPACE, endpointInterface=UsersService.INTERFACE)
public class SoapUsersWebService extends SoapWebService<UsersException> implements UsersService {

	static Logger Log = Logger.getLogger(SoapUsersWebService.class.getName());
	
	final Users impl;
	public SoapUsersWebService(String domainName) {
		super( (result)-> new UsersException( result.error().toString()));
		this.impl = new JavaUsers(domainName);
	}

	@Override
	public String createUser(User user) throws UsersException {
		return super.fromJavaResult( impl.createUser(user));
	}

	@Override
	public User getUser(String name, String pwd) throws UsersException {
		return super.fromJavaResult( impl.getUser(name, pwd));
	}


	@Override
	public void verifyPassword(String name, String pwd) throws UsersException {
		super.fromJavaResult( impl.verifyPassword(name, pwd));
	}
	
	@Override
	public User updateUser(String name, String pwd, User user) throws UsersException {
		return super.fromJavaResult( impl.updateUser(name, pwd, user));
	}

	@Override
	public User deleteUser(String name, String pwd) throws UsersException {
		return super.fromJavaResult( impl.deleteUser(name, pwd));
	}

	@Override
	public List<User> searchUsers(String pattern) throws UsersException {
		return super.fromJavaResult( impl.searchUsers(pattern));
	}

}
