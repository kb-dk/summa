/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.util.schedule;

/**
 * Any class implementing this interface, can be scheduled.
 * The supported scheduling paradigme is periodic scheduling only.
 * To use scheduling create a new {@link Scheduler}
 * <p />
 * Created: 2004-06-01
 *
 * @author <a href="mailto:hal@statsbiblioteket.dk">Hans Lund</a>, Statsbiblioteket - Aarhus - Denamrk
 * @version 0.1
 * @since 0.1
 */
public interface Schedulable {


    /**
    * Gets excecuted by the {@link Scheduler} where the component is registered.
    */
    public void perform();
}




