/**
 * Copyright (c) 2009-2013, rultor.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the rultor.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rultor.client;

import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import com.jcabi.urn.URN;
import com.rexsl.test.RestTester;
import com.rultor.spi.Coordinates;
import com.rultor.spi.Pulses;
import com.rultor.spi.Spec;
import com.rultor.spi.Stand;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.CharEncoding;

/**
 * RESTful Stand.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 1.0
 */
@Immutable
@ToString
@EqualsAndHashCode(of = { "home", "token" })
@Loggable(Loggable.DEBUG)
final class RestStand implements Stand {

    /**
     * Home URI.
     */
    private final transient String home;

    /**
     * Authentication token.
     */
    private final transient String token;

    /**
     * Public ctor.
     * @param uri URI of home page
     * @param auth Authentication token
     */
    protected RestStand(final String uri, final String auth) {
        this.home = uri;
        this.token = auth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final Spec spec, final Spec widgets) {
        try {
            RestTester.start(UriBuilder.fromUri(this.home))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
                .header(HttpHeaders.AUTHORIZATION, this.token)
                .get(String.format("preparing for #update(%s)", spec))
                .assertStatus(HttpURLConnection.HTTP_OK)
                .rel("/page/links/link[@rel='save']/@href")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
                .post(
                    "#update(..)",
                    String.format(
                        "spec=%s&widgets=%s",
                        URLEncoder.encode(spec.asText(), CharEncoding.UTF_8),
                        URLEncoder.encode(widgets.asText(), CharEncoding.UTF_8)
                    )
                )
                .assertStatus(HttpURLConnection.HTTP_SEE_OTHER);
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Spec acl() {
        return new Spec.Simple(
            RestTester.start(UriBuilder.fromUri(this.home))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
                .header(HttpHeaders.AUTHORIZATION, this.token)
                .get("#spec()")
                .assertStatus(HttpURLConnection.HTTP_OK)
                .xpath("/page/stand/acl/text()")
                .get(0)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Spec widgets() {
        return new Spec.Simple(
            RestTester.start(UriBuilder.fromUri(this.home))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
                .header(HttpHeaders.AUTHORIZATION, this.token)
                .get("#widgets()")
                .assertStatus(HttpURLConnection.HTTP_OK)
                .xpath("/page/stand/widgets/text()")
                .get(0)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return RestTester.start(UriBuilder.fromUri(this.home))
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
            .header(HttpHeaders.AUTHORIZATION, this.token)
            .get("#name()")
            .assertStatus(HttpURLConnection.HTTP_OK)
            .xpath("/page/stand/name/text()")
            .get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URN owner() {
        return URN.create(
            RestTester.start(UriBuilder.fromUri(this.home))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
                .header(HttpHeaders.AUTHORIZATION, this.token)
                .get("#owner()")
                .assertStatus(HttpURLConnection.HTTP_OK)
                .xpath("/page/identity/urn/text()")
                .get(0)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pulses pulses() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void post(final Coordinates pulse, final long nano,
        final String xembly) {
        throw new UnsupportedOperationException();
    }

}
