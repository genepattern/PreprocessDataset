// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3)
// Source File Name:   ExpressionDataCreator.java

package edu.mit.broad.modules.preprocess;

import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.expr.ResExpressionData;
import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.data.matrix.IntMatrix2D;
import org.genepattern.io.ParseException;
import org.genepattern.io.expr.IExpressionDataCreator;

import java.util.Arrays;
import java.util.ArrayList;

// Referenced classes of package org.genepattern.io.expr:
//            IExpressionDataCreator

public class PreprocessExpressionDataCreator
    implements IExpressionDataCreator
{

    public PreprocessExpressionDataCreator()
    {
        this(true, true);
    }

    public PreprocessExpressionDataCreator(boolean keepRowDescriptions, boolean keepColumnDescriptions)
    {
        this.keepRowDescriptions = true;
        this.keepColumnDescriptions = true;
        this.keepRowDescriptions = keepRowDescriptions;
        this.keepColumnDescriptions = keepColumnDescriptions;
    }

    public Object create()
    {
        if(calls != null)
            return new ResExpressionData(data, calls, rowDescriptions, columnDescriptions);
        else
            return new ExpressionData(data, rowDescriptions, columnDescriptions);
    }

    public void call(int row, int column, int call)
        throws ParseException
    {
        calls.set(row, column, call);
    }

    public void data(int row, int column, double d)
        throws ParseException
    {
        data.set(row, column, d);
    }

    public void columnName(int j, String s)
        throws ParseException
    {
        data.setColumnName(j, s);
    }

    public void rowName(int i, String s)
        throws ParseException
    {
        data.setRowName(i, s);
    }

    public void rowDescription(int i, String s)
        throws ParseException
    {
        if(keepRowDescriptions)
            rowDescriptions[i] = s;
    }

    public void columnDescription(int j, String s)
        throws ParseException
    {
        if(keepColumnDescriptions)
            columnDescriptions[j] = s;
    }

    public void init(int rows, int columns, boolean hasRowDesc, boolean hasColDesc, boolean hasCalls)
        throws ParseException
    {
        data = new DoubleMatrix2D(rows, columns);

        ArrayList rowNames = new ArrayList();

        int[] rowSlice = new int[rows];
        for(int r =0; r < rows; r++)
        {
            //hack to fix duplicate names error later when row names are numeric
            String numStr = String.valueOf("!!!" + r + "==");
            data.setRowName(r, numStr);
            rowSlice[r] = r;
        }

        data = data.slice(rowSlice, null);

        if(hasRowDesc && keepRowDescriptions)
            rowDescriptions = new String[rows];
        if(hasColDesc && keepColumnDescriptions)
            columnDescriptions = new String[columns];
        if(hasCalls)
            calls = new IntMatrix2D(rows, columns);
    }

    IntMatrix2D calls;
    DoubleMatrix2D data;
    String rowDescriptions[];
    String columnDescriptions[];
    boolean keepRowDescriptions;
    boolean keepColumnDescriptions;
}

/*
package edu.mit.broad.modules.preprocess;

import org.genepattern.io.ParseException;
import org.genepattern.io.expr.ExpressionDataCreator;
import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.data.matrix.IntMatrix2D;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Jun 21, 2010
 * Time: 11:50:37 AM
 * To change this template use File | Settings | File Templates.
 */
/*public class PreprocessExpressionDataCreator extends ExpressionDataCreator
{

    public PreprocessExpressionDataCreator()
    {
        super();
    }

    public void init(int rows, int columns, boolean hasRowDesc, boolean hasColDesc, boolean hasCalls)
        throws ParseException
    {
        super.init(rows, columns, hasRowDesc, hasColDesc, hasCalls);

        long num = -90000000;
        for(int r =0; r < rows; r++)
        {
            String numStr = String.valueOf(num);
            //String numStr1 = numStr.substring(0, 3);
            //String numStr2 = numStr.substring(3, numStr.length());
            //numStr = numStr1 + "wpw!" + numStr2;
            super.rowName(r, numStr);
            num++;
        }

        System.out.println("rows: " + rows);
        ExpressionData d = (ExpressionData)this.create();
        System.out.println("row name 20041: " + d.getRowName(20040));

        System.out.println("row name 243: " + d.getRowName(243));
        System.out.println("row count: " + d.getRowCount());
    }

    public void rowName(int row, String name)
    {
        try
        {
            super.rowName(row, name);
        }
        catch(Exception e)
        {
            ExpressionData d = (ExpressionData)this.create();
            System.out.println("exception: row name is:" + d.getRowName(20040));
            e.printStackTrace();
        }
    }

    public void init()
    {
        data = new DoubleMatrix2D(rows, columns);
        System.out.println("data row names:"+ Arrays.asList(data.getRowNames()));
        if(hasRowDesc && keepRowDescriptions)
            rowDescriptions = new String[rows];
        if(hasColDesc && keepColumnDescriptions)
            columnDescriptions = new String[columns];
        if(hasCalls)
            calls = new IntMatrix2D(rows, columns);
    }
}  */
