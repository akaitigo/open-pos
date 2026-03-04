package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import openpos.product.v1.CreateCategoryRequest
import openpos.product.v1.DeleteCategoryRequest
import openpos.product.v1.ListCategoriesRequest
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.UpdateCategoryRequest

@Path("/api/categories")
@Blocking
class CategoryResource {
    @Inject
    @GrpcClient("product-service")
    lateinit var stub: ProductServiceGrpc.ProductServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @POST
    fun create(body: CreateCategoryBody): Response {
        val request =
            CreateCategoryRequest
                .newBuilder()
                .setName(body.name)
                .apply {
                    body.parentId?.let { setParentId(it) }
                    body.color?.let { setColor(it) }
                    body.icon?.let { setIcon(it) }
                    body.displayOrder?.let { setDisplayOrder(it) }
                }.build()
        val response = grpc.withTenant(stub).createCategory(request)
        return Response.status(Response.Status.CREATED).entity(response.category.toMap()).build()
    }

    @GET
    fun list(
        @QueryParam("parentId") parentId: String?,
    ): List<Map<String, Any?>> {
        val request =
            ListCategoriesRequest
                .newBuilder()
                .apply { parentId?.let { setParentId(it) } }
                .build()
        return grpc
            .withTenant(stub)
            .listCategories(request)
            .categoriesList
            .map { it.toMap() }
    }

    @PUT
    @Path("/{id}")
    fun update(
        @PathParam("id") id: String,
        body: UpdateCategoryBody,
    ): Map<String, Any?> {
        val request =
            UpdateCategoryRequest
                .newBuilder()
                .setId(id)
                .apply {
                    body.name?.let { setName(it) }
                    body.parentId?.let { setParentId(it) }
                    body.color?.let { setColor(it) }
                    body.icon?.let { setIcon(it) }
                    body.displayOrder?.let { setDisplayOrder(it) }
                }.build()
        return grpc
            .withTenant(stub)
            .updateCategory(request)
            .category
            .toMap()
    }

    @DELETE
    @Path("/{id}")
    fun delete(
        @PathParam("id") id: String,
    ): Response {
        grpc.withTenant(stub).deleteCategory(DeleteCategoryRequest.newBuilder().setId(id).build())
        return Response.noContent().build()
    }
}

data class CreateCategoryBody(
    val name: String,
    val parentId: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val displayOrder: Int? = null,
)

data class UpdateCategoryBody(
    val name: String? = null,
    val parentId: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val displayOrder: Int? = null,
)
