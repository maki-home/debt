package am.ik.home;

import java.io.UncheckedIOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestTokenGenerator {
	private final String accessToken;
	private final String tokenKey;

	public TestTokenGenerator() {
		JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
		converter.setVerifierKey("-----BEGIN PUBLIC KEY-----\n"
				+ "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApSmVdhRnVGlYsT3kYkxU\n"
				+ "R780Amka0qafzl2+qNRWVWcknhVxCZAJ4Y4tQd7D8RpBTbHTQw5jO/gYMKyfLtrM\n"
				+ "fTv05XXT4I+f2TmspleWOSEQSNvQYfjPjto+/1GkDC0OJTn7xe0oSFuGySu9XJxW\n"
				+ "emhNZH9beP1N7shw2uX2a6fO6jo/E0S/X0SxIsaZJxyzYoeEc6iovjv1+orC+HZ/\n"
				+ "gqiT4q0SiRwO72VSu3OmwY95z8J0P4LtpzJfvTok0JreJFv4hauhuxU/qLeDDgga\n"
				+ "CvYxNRyattFQX+ivXnUcLCt7cwaNvvJSjY6dGLmKhbw7nKwmdpRgKV/s4yhWHS+s\n"
				+ "oQIDAQAB\n" + "-----END PUBLIC KEY-----");
		converter.setSigningKey("-----BEGIN RSA PRIVATE KEY-----\n"
				+ "MIIEpAIBAAKCAQEApSmVdhRnVGlYsT3kYkxUR780Amka0qafzl2+qNRWVWcknhVx\n"
				+ "CZAJ4Y4tQd7D8RpBTbHTQw5jO/gYMKyfLtrMfTv05XXT4I+f2TmspleWOSEQSNvQ\n"
				+ "YfjPjto+/1GkDC0OJTn7xe0oSFuGySu9XJxWemhNZH9beP1N7shw2uX2a6fO6jo/\n"
				+ "E0S/X0SxIsaZJxyzYoeEc6iovjv1+orC+HZ/gqiT4q0SiRwO72VSu3OmwY95z8J0\n"
				+ "P4LtpzJfvTok0JreJFv4hauhuxU/qLeDDggaCvYxNRyattFQX+ivXnUcLCt7cwaN\n"
				+ "vvJSjY6dGLmKhbw7nKwmdpRgKV/s4yhWHS+soQIDAQABAoIBAQCfB+tiR0M3hDhi\n"
				+ "cbX28Ig2jWUG7S1mqAo4KwvqhIqcsTQRA5RUIN1i0gPB6T3CagV3SkKhzri+haTF\n"
				+ "OXINK6C69YBZcZsSHzlLcL1Nbgi3+Z40iXOc4nb4li0SqwnKr5dvQOWtLy4afgbK\n"
				+ "rdMn2WFrfAMJQZhSZ5Ui1t3T6JneDanGb26zUEi3o2mU5qIaKLKSadifZdh3/P/x\n"
				+ "O87eqtdxr0s//00gRLg+8YRr2wNWYTZdpHh5WlsPVnxQ2lZskhtH7UdvseOTvykR\n"
				+ "0UwIAPLGv7sjAZRJUBhaYDFK3rkgIXgE6lQ7Tdq/GeZKrSKDR54JeaZ9a8U/ag4T\n"
				+ "mBnxyhoBAoGBANwfdeUXvVsFBJDHBKQQTi7RvwfUimftTFOctZZ/WyVQxmffMz1a\n"
				+ "HXAfFhl8iH4qiGsF9bRW49cRgQGnOF1W2m4BKm5OKIDi+152i5X5GkhQfnj2BmtV\n"
				+ "tD50QTBr5b0avcB0SQMDjybWTXVAKAV0FcaS1aRWgvYXID7v6Hg4izwHAoGBAMAU\n"
				+ "65m2NeH70vHqOz5Ni71yB+w6IFz+zeoZC/TNxfj5xmdHMqO5kTbx5IABxuC69/J+\n"
				+ "Ix5Ld0gBf/1XmkkBYFdd1hYK4dEdBeaUViAgj1VVAVGMuxarey5O3XPePllcWvBS\n"
				+ "+34d6Q2LrpHjZPBcg4fsJLk0PtbxV8VTrcRopngXAoGBANFsAQ9PcbC56mkPX4Pz\n"
				+ "m16B4PxDItbTdi2KR10Cl6W93rWOLcKuDJnSiYT/7SATuSwRRH0puaSxG4qGHGL5\n"
				+ "MeE8rLC7HV/aE4sAv7aUg4PPSTQt5VeS92P/2NGHpdAvM955A8mhRj2N362wtyLR\n"
				+ "GbKNmkK6p+gXgm8+cm6GZTlbAoGAeEZhRI3Y7Zlu/DYsRJXwE38swNmg3DpdG+H1\n"
				+ "H6Qd/AoDFzZ42hZffKU47IKwUpYgngIn4Vdl6Huv9iX9oQswlWRXotPLlNJ0mG54\n"
				+ "A4P/3HHu8m6J0+cqqdOOTXhzWCdBdkyv18fI38eAVy+pS+zSG7FdSW8SjirvR8te\n"
				+ "+qaA3p8CgYApR921C3dl1b5mtvSg49/mQOrBfZGgGrWpXnJxGDo+goSimAPLipnq\n"
				+ "nZzW6X3gjr7iTpw418dyedbieeolCIWBqV9JmrwJtopIxS4jmYG5HUs7jpF6bcro\n"
				+ "HKWNkvcndduYb5M6+2ZBxCW3kRD/onWTESoAMyPCbNhm21w3WGGAdA==\n"
				+ "-----END RSA PRIVATE KEY-----");
		Set<String> scope = new HashSet<>();
		scope.add("read");
		scope.add("write");
		Map<String, String> params = new HashMap<>();
		params.put("grant_type", "password");
		params.put("username", "maki@example.com");
		OAuth2Request oAuth2Request = new OAuth2Request(params, "foo", null, true, scope,
				null, null, null, null);
		DefaultOAuth2AccessToken accessToken = new DefaultOAuth2AccessToken(
				UUID.randomUUID().toString());
		accessToken.setExpiration(Timestamp.valueOf(LocalDateTime.now().plusDays(1)));
		accessToken.setScope(scope);

		Map<String, Object> info = new HashMap<>();
		info.put("user_id", "00000000-0000-0000-0000-000000000000");
		info.put("email", "maki@example.com");
		info.put("display_name", "Maki Toshiaki");

		accessToken.setAdditionalInformation(info);

		OAuth2Authentication authentication = new OAuth2Authentication(oAuth2Request,
				new UsernamePasswordAuthenticationToken("maki@example.com", "password"));
		OAuth2AccessToken enhance = converter.enhance(accessToken, authentication);
		this.accessToken = enhance.getValue();
		try {
			this.tokenKey = new ObjectMapper().writeValueAsString(converter.getKey());
		}
		catch (JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getTokenKey() {
		return tokenKey;
	}
}