package com.sksamuel.elastic4s.http.search.queries.geo

import com.sksamuel.elastic4s.searches.queries.geo.GeoPolygonQueryDefinition
import org.elasticsearch.common.xcontent.{XContentBuilder, XContentFactory}

object GeoPolyonQueryBodyFn {

  def apply(q: GeoPolygonQueryDefinition): XContentBuilder = {
    val builder = XContentFactory.jsonBuilder()
    builder.startObject()
    builder.startObject("geo_polygon")
    builder.startObject(q.field)

    builder.startArray("points")
    q.points.foreach { point =>
      builder.startObject()
      builder.field("lat", point.lat)
      builder.field("lon", point.lon)
      builder.endObject()
    }
    builder.endArray()

    q.ignoreUnmapped.foreach(builder.field("ignore_unmapped", _))
    q.validationMethod.map(_.name).foreach(builder.field("validation_method", _))
    q.boost.foreach(builder.field("boost", _))
    q.queryName.foreach(builder.field("_name", _))

    builder.endObject()
    builder.endObject()
    builder.endObject()
    builder
  }
}
