/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.network.manager.handler.base;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.nuls.base.data.BaseNulsData;
import io.nuls.network.model.NetworkEventResult;
import io.nuls.network.model.Node;
import io.nuls.network.model.message.base.BaseMessage;
import io.nuls.network.model.message.base.MessageHeader;
import io.nuls.tools.constant.ToolsConstant;
import io.nuls.tools.log.Log;

/**
 * message handler
 * @author  lan
 * @date 2018/11/01
 */
public abstract  class BaseMessageHandler implements BaseMeesageHandlerInf {
    protected  static final int PlaceHolderSize = ToolsConstant.PLACE_HOLDER.length;
    @Override
    public NetworkEventResult send(BaseMessage message, Node node, boolean isServer, boolean asyn) {
        try {
            MessageHeader header = message.getHeader();
            header.setMagicNumber(header.getMagicNumber());
            BaseNulsData body = message.getMsgBody();
            header.setPayloadLength(body.size());
            ChannelFuture future = node.getChannel().writeAndFlush(Unpooled.wrappedBuffer(message.serialize()));
            if (!asyn) {
                future.await();
                boolean success = future.isSuccess();
                if (!success) {
                    return new NetworkEventResult(false, null);
                }
            }
        } catch (Exception e) {
            Log.error(e);
            return new NetworkEventResult(false,null);
        }
        return new NetworkEventResult(true, null);
    }
}
