package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2017, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataset.DataInputPeriod;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.period.PeriodType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DataSetObjectBundleHook extends AbstractObjectBundleHook
{
    @Override
    public List<ErrorReport> validate( IdentifiableObject object, ObjectBundle bundle )
    {
        List<ErrorReport> errorList = new ArrayList<>();

        if ( !DataSet.class.isInstance( object ) ) return errorList;
        DataSet dataSet = (DataSet) object;

        if ( !dataSet.getDataInputPeriods().isEmpty() )
        {
            List<ErrorReport> dataInputPeriods = dataSet.getDataInputPeriods().stream()
                .map( dataInputPeriod ->
                {
                    DataInputPeriod dip = bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataInputPeriod );

                    if ( dip == null )
                    {
                        preheatService.connectReferences( dataInputPeriod, bundle.getPreheat(), bundle.getPreheatIdentifier() );
                        return dataInputPeriod;
                    }

                    return dip;
                } )
                .filter( dataInputPeriod -> !dataInputPeriod.getPeriod().getPeriodType().equals( dataSet.getPeriodType() ) )
                .map( dataInputPeriod -> new ErrorReport( object.getClass(), ErrorCode.E4012, "dataInputPeriods" ) )
                .collect( Collectors.toList() );

            errorList.addAll( dataInputPeriods );
        }

        return errorList;
    }

    @Override
    public void preCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !DataSet.class.isInstance( object ) ) return;
        DataSet dataSet = (DataSet) object;

        if ( dataSet.getPeriodType() != null )
        {
            PeriodType periodType = bundle.getPreheat().getPeriodTypeMap().get( dataSet.getPeriodType().getName() );
            dataSet.setPeriodType( periodType );
        }
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !DataSet.class.isInstance( object ) ) return;
        DataSet dataSet = (DataSet) object;

        if ( dataSet.getPeriodType() != null )
        {
            PeriodType periodType = bundle.getPreheat().getPeriodTypeMap().get( dataSet.getPeriodType().getName() );
            dataSet.setPeriodType( periodType );
        }

        sessionFactory.getCurrentSession().flush();
    }
}
