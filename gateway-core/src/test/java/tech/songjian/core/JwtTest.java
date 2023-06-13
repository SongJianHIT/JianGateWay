package tech.songjian.core;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Test;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * tech.songjian
 *
 * @Author: SongJian
 * @Create: 2023/6/13 10:26
 * @Version:
 * @Describe:
 */
public class JwtTest {

    @Test
    public void jwt() {
        String secureKey = "jianjian";

        String token = Jwts.builder()
                .setSubject("10000")
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256, secureKey)
                .compact();
        System.out.println(token);
    }
}
