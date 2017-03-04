package am.ik.debt.security;

import java.security.Principal;

import am.ik.home.client.user.UaaUser;

public class UserPrincipal implements Principal {
	private final UaaUser uaaUser;
	private final String token;

	public UserPrincipal(UaaUser uaaUser, String token) {
		this.uaaUser = uaaUser;
		this.token = token;
	}

	public UaaUser getUaaUser() {
		return uaaUser;
	}

	public String getToken() {
		return token;
	}

	@Override
	public String getName() {
		return uaaUser.getDisplayName();
	}

	@Override
	public String toString() {
		return uaaUser.toString();
	}
}
