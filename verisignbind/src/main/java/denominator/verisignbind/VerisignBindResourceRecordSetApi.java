package denominator.verisignbind;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import denominator.ResourceRecordSetApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.verisignbind.VerisignBindAdapters.ResourceRecord;

final class VerisignBindResourceRecordSetApi implements ResourceRecordSetApi {

  private final VerisignBind api;
  private final String zoneName;

  VerisignBindResourceRecordSetApi(VerisignBind api, String zoneName) {
    this.api = api;
    this.zoneName = zoneName;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    return new GroupByRecordNameAndTypeIterator(api.getResourceRecords(zoneName).iterator());
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    // return filter(iterator(), nameEqualTo(name));
    // ResourceRecord record = api.getResourceRecord(zoneName, name, null);
    // List<ResourceRecord> records = new ArrayList<VerisignBindAdapters.ResourceRecord>();
    // records.add(record);
    // return new GroupByRecordNameAndTypeIterator(records.iterator());

    return null;

    // Builder<Map<String, Object>> builder =
    // ResourceRecordSet.builder().name(name).type(record.getType()).ttl(record.getTtl());
    // builder.add(getRRTypeAndRdata(record.getType(), record.getRdata()));
    // builder.build();
  }

  @Override
  public ResourceRecordSet<?> getByNameAndType(String name, String type) {
    // ResourceRecord record = api.getResourceRecord(zoneName, name, type);
    // Builder<Map<String, Object>> builder =
    // ResourceRecordSet.builder().name(name).type(record.getType()).ttl(record.getTtl());
    // builder.add(getRRTypeAndRdata(record.getType(), record.getRdata()));
    // return builder.build();
    List<ResourceRecord> records = api.getResourceRecord(zoneName, name, type);
    ResourceRecord record = records.get(0);
    Builder<Map<String, Object>> builder =
        ResourceRecordSet.builder().name(name).type(record.getType()).ttl(record.getTtl());
    builder.add(getRRTypeAndRdata(record.getType(), record.getRdata()));
    return builder.build();
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(!rrset.records().isEmpty(), "rrset was empty %s", rrset);

    List<Map<String, Object>> recordsToCreate = new ArrayList<Map<String, Object>>(rrset.records());

    // for (ResourceRecord record : api.getResourceRecords(zoneName)) {
    // if (rrset.name().equals(record.getName()) && rrset.type().equals(record.getType())) {
    // Map<String, Object> rdata = getRRTypeAndRdata(record.getType(), record.getRdata());
    // if (recordsLeftToCreate.contains(rdata)) {
    // recordsLeftToCreate.remove(rdata);
    // if (rrset.ttl() != null) {
    // if (rrset.ttl().equals(record.getTtl())) {
    // continue;
    // }
    // record.setTtl(rrset.ttl());
    // api.updateResourceRecord(zoneName, record.getName(), record.getTtl(),
    // Arrays.asList(record.getRdata()));
    // }
    // } else {
    // api.deleteResourceRecord(zoneName, record.getName(), record.getType());
    // }
    // }
    // }

    ResourceRecord record = new ResourceRecord();
    record.setName(rrset.name());
    record.setType(rrset.type());

    if (rrset.ttl() != null) {
      record.setTtl(rrset.ttl());
    } else {
      record.setTtl(86400);
    }

    // List<String> list = new ArrayList<String>();
    for (Map<String, Object> rdata : recordsToCreate) {
      LinkedHashMap<String, Object> mutable = new LinkedHashMap<String, Object>(rdata);
      record.setRdata(join(' ', mutable.values().toArray()));
      // list.add(join(' ', mutable.values().toArray()));

      api.createResourceRecord(zoneName, record.getName(), record.getType(), record.getTtl(),
          record.getRdata());

    }


    // System.out.println(buildRdata(list));
    // api.createResourceRecord(zoneName, record.getName(), record.getType(), record.getTtl(),
    // buildRdata(list));
  }

  private String buildRdata(List<String> list) {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    for (String rdata : list) {
      builder.append("\"");
      builder.append(rdata);
      builder.append("\",");
    }

    builder.append("]");
    String rdata = builder.toString();
    int index = rdata.lastIndexOf(",");
    if (index == -1) {
      return rdata;
    }

    return new StringBuilder(rdata).replace(index, index + 1, "").toString();
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");
    for (ResourceRecord record : api.getResourceRecords(zoneName)) {
      if (name.equals(record.getName()) && type.equals(record.getType())) {
        api.deleteResourceRecord(zoneName, name, record.getType());
      }
    }
  }

  static final class Factory implements denominator.ResourceRecordSetApi.Factory {
    private final VerisignBind api;

    @Inject
    Factory(VerisignBind api) {
      this.api = checkNotNull(api, "api");
    }

    @Override
    public ResourceRecordSetApi create(String name) {
      return new VerisignBindResourceRecordSetApi(api, name);
    }
  }

  public static Map<String, Object> getRRTypeAndRdata(String type, String rdata) {
    rdata = rdata.replace("\"", "");
    return Util.toMap(type, rdata);
  }
}
