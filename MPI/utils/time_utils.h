#pragma once
#include <string>
#include <vector>

namespace TimeUtils
{
    // Converts "08:00" to 480 (minutes from midnight)
    inline int toMinutes(const std::string &time)
    {
        int h = std::stoi(time.substr(0, 2));
        int m = std::stoi(time.substr(3, 2));
        return h * 60 + m;
    }

    // Checks if two time intervals overlap
    // Interval A: [start1, end1), Interval B: [start2, end2)
    inline bool overlap(const std::string &start1, const std::string &end1,
                        const std::string &start2, const std::string &end2)
    {
        int s1 = toMinutes(start1);
        int e1 = toMinutes(end1);
        int s2 = toMinutes(start2);
        int e2 = toMinutes(end2);

        // Standard overlap logic: (StartA < EndB) and (StartB < EndA)
        return (s1 < e2 && s2 < e1);
    }
}