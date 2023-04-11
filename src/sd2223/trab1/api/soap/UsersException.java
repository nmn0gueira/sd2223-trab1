package sd2223.trab1.api.soap;

import jakarta.xml.ws.WebFault;

import java.io.Serial;

@WebFault
public class UsersException extends Exception {


	public UsersException() {
		super("");
	}

	public UsersException(String errorMessage ) {
		super(errorMessage);
	}
	
	@Serial
	private static final long serialVersionUID = 1L;
}
