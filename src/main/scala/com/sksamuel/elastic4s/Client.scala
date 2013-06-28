package com.sksamuel.elastic4s

import scala.concurrent._
import org.elasticsearch.action.index.{IndexRequest, IndexResponse}
import org.elasticsearch.action.count.{CountRequest, CountResponse}
import org.elasticsearch.action.search.{SearchRequest, SearchResponse}
import org.elasticsearch.action.admin.indices.validate.query.{ValidateQueryResponse, ValidateQueryRequest}
import org.elasticsearch.action.mlt.MoreLikeThisRequest
import org.elasticsearch.common.settings.{Settings, ImmutableSettings}
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.node.{Node, NodeBuilder}
import org.elasticsearch.client.Client
import org.elasticsearch.action.get.{GetResponse, GetRequest}
import org.elasticsearch.action.delete.{DeleteResponse, DeleteRequest}
import org.elasticsearch.action.deletebyquery.{DeleteByQueryRequest, DeleteByQueryResponse}
import org.elasticsearch.action.update.{UpdateResponse, UpdateRequest}
import scala.concurrent.duration._
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.percolate.PercolateResponse
import ElasticDsl._
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse

/** @author Stephen Samuel */
class ElasticClient(val client: org.elasticsearch.client.Client, timeout: Long)
                   (implicit executionContext: ExecutionContext = ExecutionContext.global) {

    /**
     * Indexes a Java IndexRequest and returns a scala Future with the IndexResponse.
     *
     * @param req an IndexRequest from the Java client
     *
     * @return a Future providing an IndexResponse
     */
    def execute(req: IndexRequest): Future[IndexResponse] = future {
        client.index(req).actionGet(timeout)
    }

    /**
     * Indexes a Scala DSL IndexBuilder and returns a scala Future with the IndexResponse.
     *
     * @param builder an IndexBuilder from the Scala DSL
     *
     * @return a Future providing an IndexResponse
     */
    def execute(builder: IndexBuilder): Future[IndexResponse] = future {
        client.index(builder.java).actionGet(timeout)
    }

    /**
     * Executes a Java API SearchRequest and returns a scala Future with the SearchResponse.
     *
     * @param req a SearchRequest from the Java client
     *
     * @return a Future providing an SearchResponse
     */
    def execute(req: SearchRequest): Future[SearchResponse] = future {
        client.search(req).actionGet(timeout)
    }

    /**
     * Executes a Scala DSL search and returns a scala Future with the SearchResponse.
     *
     * @param builder a SearchBuilder from the Scala DSL
     *
     * @return a Future providing an SearchResponse
     */
    def execute(builder: SearchBuilder): Future[SearchResponse] = future {
        client.search(builder.build).actionGet(timeout)
    }

    def result(builder: SearchBuilder)(implicit duration: Duration): SearchResponse =
        Await.result(execute(builder), duration)

    /**
     * Executes a Java API CountRequest and returns a scala Future with the CountResponse.
     *
     * @param req a CountRequest from the Java client
     *
     * @return a Future providing an CountResponse
     */
    def execute(req: CountRequest): Future[CountResponse] = future {
        client.count(req).actionGet(timeout)
    }

    /**
     * Executes a Scala DSL search and returns a scala Future with the CountResponse.
     *
     * @param builder a CountBuilder from the Scala DSL
     *
     * @return a Future providing an CountResponse
     */
    def execute(builder: CountBuilder): Future[CountResponse] = execute(builder.build)

    /**
     * Executes a Java API GetRequest and returns a scala Future with the GetResponse.
     *
     * @param req a GetRequest from the Java client
     *
     * @return a Future providing an GetResponse
     */
    def execute(req: GetRequest): Future[GetResponse] = future {
        client.get(req).actionGet(timeout)
    }

    /**
     * Executes a Scala DSL get and returns a scala Future with the GetResponse.
     *
     * @param builder a GetDefinition from the Scala DSL
     *
     * @return a Future providing an GetResponse
     */
    def execute(builder: GetDefinition): Future[GetResponse] = execute(builder.build)

    def execute(req: DeleteRequest): Future[DeleteResponse] = future {
        client.delete(req).actionGet(timeout)
    }

    def execute(create: CreateIndexDefinition): Future[CreateIndexResponse] = future {
        client.admin.indices.create(create.build).actionGet(timeout)
    }

    def execute(d: DeleteByIdDefinition): Future[DeleteResponse] = future {
        client.delete(d.builder).actionGet(timeout)
    }

    def execute(req: DeleteByQueryRequest): Future[DeleteByQueryResponse] = future {
        client.deleteByQuery(req).actionGet(timeout)
    }
    def execute(d: DeleteByQueryDefinition): Future[DeleteByQueryResponse] = execute(d.builder)
    def result(d: DeleteByQueryDefinition)(implicit duration: Duration): DeleteByQueryResponse =
        Await.result(execute(d), duration)

    def execute(req: ValidateQueryRequest): Future[ValidateQueryResponse] = future {
        client.admin.indices().validateQuery(req).actionGet(timeout)
    }

    def execute(validateDef: ValidateDefinition): Future[ValidateQueryResponse] = execute(validateDef.build)
    def result(validateDef: ValidateDefinition)(implicit duration: Duration): ValidateQueryResponse =
        Await.result(execute(validateDef.build), duration)

    def execute(req: UpdateRequest): Future[UpdateResponse] = future {
        client.update(req).actionGet(timeout)
    }

    def execute(updateDef: UpdateDefinition): Future[UpdateResponse] = execute(updateDef.build)
    def result(updateDef: UpdateDefinition)(implicit duration: Duration): UpdateResponse =
        Await.result(execute(updateDef.build), duration)

    def execute(req: MoreLikeThisRequest): Future[SearchResponse] = future {
        client.moreLikeThis(req).actionGet(5000)
    }
    def execute(mltDef: MoreLikeThisDefinition): Future[SearchResponse] = execute(mltDef._builder)
    def result(mltDef: MoreLikeThisDefinition)(implicit duration: Duration): SearchResponse =
        Await.result(execute(mltDef), duration)

    def execute(requests: BulkCompatibleRequest*): Future[BulkResponse] = {
        val bulk = client.prepareBulk()
        requests.foreach(req => req match {
            case index: IndexBuilder => bulk.add(index.java)
            case delete: DeleteByIdDefinition => bulk.add(delete.builder)
            case update: UpdateDefinition => bulk.add(update.build)
        })
        future {
            bulk.execute().actionGet(timeout)
        }
    }
    def result(requests: BulkCompatibleRequest*)(implicit duration: Duration): BulkResponse =
        Await.result(execute(requests: _*), duration)

    def exists(indexes: Iterable[String]): Future[IndicesExistsResponse] = future {
        client.admin().indices().prepareExists(indexes.toSeq: _*).execute().actionGet(timeout)
    }

    def register(registerDef: RegisterDefinition): Future[IndexResponse] = execute(registerDef.build.request)
    def registerSync(registerDef: RegisterDefinition)(implicit duration: Duration): IndexResponse =
        Await.result(register(registerDef), duration)

    def percolate(percolate: PercolateDefinition): Future[PercolateResponse] = future {
        client.percolate(percolate.build).actionGet(timeout)
    }

    def searchScroll(scrollId: String): Future[SearchResponse] = future {
        client.prepareSearchScroll(scrollId).execute().actionGet(timeout)
    }

    def close(): Unit = client.close()

    def admin = client.admin

    def sync(implicit duration: Duration = 10.seconds) = new SyncClient(this)(duration)

    class SyncClient(client: ElasticClient)(implicit duration: Duration) {

        def percolate(percolateDef: PercolateDefinition)(implicit duration: Duration): PercolateResponse =
            Await.result(client.percolate(percolateDef), duration)

        def register(registerDef: RegisterDefinition)(implicit duration: Duration): IndexResponse =
            Await.result(client.register(registerDef), duration)

        def execute(getDef: GetDefinition)(implicit duration: Duration): GetResponse =
            Await.result(client.execute(getDef), duration)
    }
}

object ElasticClient {

    val DefaultTimeout = 5000

    def fromClient(client: Client): ElasticClient = fromClient(client, DefaultTimeout)
    def fromClient(client: Client, timeout: Long = DefaultTimeout): ElasticClient = new ElasticClient(client, timeout)
    def fromNode(node: Node): ElasticClient = fromNode(node, DefaultTimeout)
    def fromNode(node: Node, timeout: Long = DefaultTimeout): ElasticClient = fromClient(node.client, timeout)

    def remote(host: String = "localhost", ports: Int*)(timeout: Long = DefaultTimeout): ElasticClient =
        remote(ImmutableSettings.builder().build(), host, ports: _*)(timeout)
    def remote(settings: Settings, host: String = "localhost", ports: Int*)(timeout: Long = DefaultTimeout): ElasticClient = {
        require(settings.getAsMap.containsKey("cluster.name"))
        val client = new TransportClient(settings)
        for ( port <- ports ) client.addTransportAddress(new InetSocketTransportAddress(host, port))
        fromClient(client, timeout)
    }

    def local: ElasticClient = local(ImmutableSettings.settingsBuilder().build())
    def local(settings: Settings, timeout: Long = DefaultTimeout): ElasticClient =
        fromNode(NodeBuilder.nodeBuilder().local(true).data(true).settings(settings).node())

}

sealed abstract class SearchOperationThreading(elastic: org.elasticsearch.action.search.SearchOperationThreading)
object SearchOperationThreading {
    case object NoThreads extends SearchOperationThreading(org.elasticsearch.action.search.SearchOperationThreading.NO_THREADS)
    case object SingleThread extends SearchOperationThreading(org.elasticsearch.action.search.SearchOperationThreading.SINGLE_THREAD)
    case object ThreadPerShard extends SearchOperationThreading(org.elasticsearch.action.search.SearchOperationThreading.THREAD_PER_SHARD)
}

