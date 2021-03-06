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
package com.rultor.users;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.jcabi.aspects.Tv;
import com.jcabi.dynamo.Credentials;
import com.jcabi.dynamo.Region;
import com.jcabi.dynamo.TableMocker;
import com.jcabi.urn.URN;
import com.rultor.aws.SQSClient;
import com.rultor.spi.Rule;
import com.rultor.spi.Rules;
import com.rultor.spi.Spec;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Integration case for {@link AwsRules}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
public final class AwsRulesITCase {

    /**
     * TCP port of DynamoDB Local.
     */
    private static final int PORT = Integer.parseInt(
        System.getProperty("dynamodb.port")
    );

    /**
     * Region to work with.
     */
    private transient Region region;

    /**
     * Table mocker to work with.
     */
    private transient TableMocker table;

    /**
     * Assume we're online.
     * @throws Exception If fails
     */
    @Before
    public void prepare() throws Exception {
        this.region = new Region.Simple(
            new Credentials.Direct(Credentials.TEST, AwsRulesITCase.PORT)
        );
        this.table = new TableMocker(
            this.region,
            new CreateTableRequest()
                .withTableName(AwsRule.TABLE)
                .withProvisionedThroughput(
                    new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L)
                )
                .withAttributeDefinitions(
                    new AttributeDefinition()
                        .withAttributeName(AwsRule.HASH_OWNER)
                        .withAttributeType(ScalarAttributeType.S),
                    new AttributeDefinition()
                        .withAttributeName(AwsRule.RANGE_NAME)
                        .withAttributeType(ScalarAttributeType.S)
                )
                .withKeySchema(
                    new KeySchemaElement()
                        .withAttributeName(AwsRule.HASH_OWNER)
                        .withKeyType(KeyType.HASH),
                    new KeySchemaElement()
                        .withAttributeName(AwsRule.RANGE_NAME)
                        .withKeyType(KeyType.RANGE)
                )
        );
        this.table.create();
    }

    /**
     * Assume we're online.
     * @throws Exception If fails
     */
    @After
    public void drop() throws Exception {
        this.table.drop();
    }

    /**
     * AwsRules can add rules and list them.
     * @throws Exception If some problem inside
     */
    @Test
    public void addsRulesAndListsThem() throws Exception {
        final Rules rules = new AwsRules(
            this.region, Mockito.mock(SQSClient.class), new URN("urn:test:6")
        );
        final String name = "test-rule";
        rules.create(name);
        MatcherAssert.assertThat(rules.contains(name), Matchers.is(true));
        MatcherAssert.assertThat(rules.contains("another"), Matchers.is(false));
        MatcherAssert.assertThat(
            Iterables.filter(
                rules,
                new Predicate<Rule>() {
                    @Override
                    public boolean apply(final Rule rule) {
                        return rule.name().equals(name)
                            && rule.failure().isEmpty();
                    }
                }
            ),
            Matchers.<Rule>iterableWithSize(1)
        );
        final Rule rule = rules.get(name);
        MatcherAssert.assertThat(rule.failure(), Matchers.equalTo(""));
    }

    /**
     * AwsRules can set default attributes of a new rule.
     * @throws Exception If some problem inside
     */
    @Test
    public void setsDefaultAttributesOfNewRule() throws Exception {
        final Rules rules = new AwsRules(
            this.region, Mockito.mock(SQSClient.class), new URN("urn:test:7")
        );
        final String name = "test-rule-new";
        rules.create(name);
        final Rule rule = rules.get(name);
        MatcherAssert.assertThat(rule.failure(), Matchers.equalTo(""));
        MatcherAssert.assertThat(
            rule.spec(),
            Matchers.<Spec>equalTo(new Spec.Simple())
        );
        MatcherAssert.assertThat(
            rule.drain(),
            Matchers.<Spec>equalTo(new Spec.Simple("com.rultor.drain.Trash()"))
        );
    }

    /**
     * AwsRules can add many rules and then delete them.
     * @throws Exception If some problem inside
     */
    @Test
    public void createsManyRulesAndRemovesThem() throws Exception {
        final Rules rules = new AwsRules(
            this.region, Mockito.mock(SQSClient.class), new URN("urn:test:9")
        );
        final Collection<String> names = new LinkedList<String>();
        for (int idx = 0; idx < Tv.TWENTY; ++idx) {
            final String name = RandomStringUtils.randomAlphabetic(Tv.THIRTY)
                .toLowerCase(Locale.ENGLISH);
            rules.create(name);
            names.add(name);
        }
        for (String name : names) {
            MatcherAssert.assertThat(rules.contains(name), Matchers.is(true));
        }
        for (String name : names) {
            rules.remove(name);
        }
        for (String name : names) {
            MatcherAssert.assertThat(rules.contains(name), Matchers.is(false));
        }
    }

}
