package test.model;

import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

@MappedSuperclass
public abstract class BaseWithEqualsAndNoIdEntity {


  @Version
  Long version;

  private transient int equalsCount;
  
  public String toString() {
    return ""+equalsCount;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public boolean equals(Object obj) {
    equalsCount++;
    return (obj != null);
  }
  

}
