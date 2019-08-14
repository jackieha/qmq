/*
 * Copyright 2018 Qunar, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package qunar.tc.qmq.store;

import qunar.tc.qmq.store.buffer.SegmentBuffer;
import qunar.tc.qmq.utils.PayloadHolderUtils;

import java.nio.ByteBuffer;

/**
 * @author keli.wang
 * @since 2017/8/21
 */
public class IndexLogVisitor extends AbstractLogVisitor<MessageQueryIndex> {

    IndexLogVisitor(final LogManager logManager, final long startOffset) {
        super(logManager, startOffset);
    }

    @Override
    protected LogVisitorRecord<MessageQueryIndex> readOneRecord(SegmentBuffer segmentBuffer) {
        final ByteBuffer buffer = segmentBuffer.getBuffer();
        final int startPos = buffer.position();
        // magic
        if (buffer.remaining() < Long.BYTES) {
            //end of file
            return LogVisitorRecord.noMore();
        }

        // sequence
        long sequence = buffer.getLong();
        if (sequence < 0) {
            return LogVisitorRecord.noMore();
        }

        // createTime
        if (buffer.remaining() < Long.BYTES) {
            return LogVisitorRecord.noMore();
        }
        long createTime = buffer.getLong();

        // subject
        if (buffer.remaining() < Short.BYTES) {
            return LogVisitorRecord.noMore();
        }
        short subjectLen = buffer.getShort();
        if (buffer.remaining() < subjectLen) {
            return LogVisitorRecord.noMore();
        }
        String subject = PayloadHolderUtils.readString(subjectLen, buffer);

        // msgId
        if (buffer.remaining() < Short.BYTES) {
            return LogVisitorRecord.noMore();
        }
        short messageIdLen = buffer.getShort();
        if (buffer.remaining() < messageIdLen) {
            return LogVisitorRecord.noMore();
        }
        String messageId = PayloadHolderUtils.readString(messageIdLen, buffer);

        MessageQueryIndex index = new MessageQueryIndex(subject, messageId, sequence, createTime);
        incrVisitedBufferSize(buffer.position() - startPos);
        index.setCurrentOffset(getStartOffset() + visitedBufferSize());
        return LogVisitorRecord.data(index);
    }

}
