package com.openpos.gateway.config

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.MethodDescriptor
import io.grpc.stub.AbstractBlockingStub
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class GrpcClientHelperTest {
    @Test
    fun `withTenant uses configured deadline seconds`() {
        val helper =
            GrpcClientHelper().apply {
                grpcDeadlineSeconds = 30
            }
        val stub = TestBlockingStub(TestChannel(), CallOptions.DEFAULT)

        val result = helper.withTenant(stub)
        val remainingSeconds = requireNotNull(result.callOptions.deadline).timeRemaining(TimeUnit.SECONDS)

        assertTrue(remainingSeconds in 25..30, "expected remaining deadline close to 30s but was $remainingSeconds")
    }

    @Test
    fun `withTenant defaults to five seconds`() {
        val helper = GrpcClientHelper()
        val stub = TestBlockingStub(TestChannel(), CallOptions.DEFAULT)

        val result = helper.withTenant(stub)
        val remainingSeconds = requireNotNull(result.callOptions.deadline).timeRemaining(TimeUnit.SECONDS)

        assertTrue(remainingSeconds in 0..5, "expected remaining deadline close to 5s but was $remainingSeconds")
    }

    private class TestBlockingStub(
        channel: Channel,
        callOptions: CallOptions,
    ) : AbstractBlockingStub<TestBlockingStub>(channel, callOptions) {
        override fun build(
            channel: Channel,
            callOptions: CallOptions,
        ): TestBlockingStub = TestBlockingStub(channel, callOptions)
    }

    private class TestChannel : Channel() {
        override fun authority(): String = "test"

        override fun <ReqT : Any?, RespT : Any?> newCall(
            methodDescriptor: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
        ): ClientCall<ReqT, RespT> = throw UnsupportedOperationException("No real calls in unit tests")
    }
}
